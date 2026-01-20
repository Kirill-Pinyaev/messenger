package main

import (
	"context"
	"crypto/rand"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"messenger/internal/auth"
	"messenger/internal/store"
	"messenger/internal/ws"
)

func main() {
	userStore := initUserStore()
	authSvc := auth.NewService(userStore)
	msgStore := initMessageStore()
	hub := ws.NewHub(authSvc, msgStore)

	mux := http.NewServeMux()

	// Статика (web/index.html)
	webDir := filepath.Join(".", "web")
	fs := http.FileServer(http.Dir(webDir))
	mux.Handle("/", fs)

	// WebSocket endpoint
	mux.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		ws.ServeWS(hub, w, r)
	})

	mux.HandleFunc("/register", registerHandler(authSvc))
	mux.HandleFunc("/login", loginHandler(authSvc))
	mux.HandleFunc("/delete", deleteHandler(authSvc, msgStore, hub))
	mux.HandleFunc("/conversations", conversationsHandler(authSvc, msgStore, userStore))
	mux.HandleFunc("/users", usersSearchHandler(authSvc, userStore))
	mux.HandleFunc("/messages/search", messagesSearchHandler(authSvc, msgStore))
	mux.HandleFunc("/profile", profileGetHandler(authSvc, userStore))
	mux.HandleFunc("/profile/update", profileUpdateHandler(authSvc, userStore, hub))

	addr := ":8082"
	log.Println("server listening on", addr)
	log.Println("open: http://localhost:8082/")
	log.Println("demo register: curl -s -X POST http://localhost:8082/register -d '{\"username\":\"alice\",\"password\":\"secret\"}'")
	log.Println("demo login:    curl -s -X POST http://localhost:8082/login -d '{\"username\":\"alice\",\"password\":\"secret\"}'")
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatal(err)
	}
}

type credentials struct {
	Username  string `json:"username"`
	Password  string `json:"password"`
	FirstName string `json:"firstName,omitempty"`
	LastName  string `json:"lastName,omitempty"`
	AvatarHex string `json:"avatarHex,omitempty"`
	AvatarData string `json:"avatarData,omitempty"`
}

type tokenResponse struct {
	Token string `json:"token"`
}

type conversationsResponse struct {
	Items []conversationItem `json:"items"`
}

type conversationItem struct {
	ConversationID string `json:"conversation_id"`
	PeerUsername   string `json:"peer_username"`
	FirstName      string `json:"first_name"`
	LastName       string `json:"last_name"`
	AvatarHex      string `json:"avatar_hex"`
	AvatarData     string `json:"avatar_data"`
}

type usersResponse struct {
	Items []userDTO `json:"items"`
}

type userDTO struct {
	Username  string `json:"username"`
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
	AvatarHex string `json:"avatar_hex"`
	AvatarData string `json:"avatar_data"`
}

type messagesResponse struct {
	Items []messageDTO `json:"items"`
}

type messageDTO struct {
	ID             int64     `json:"message_id"`
	ConversationID string    `json:"conversation_id"`
	From           string    `json:"from"`
	To             string    `json:"to"`
	Text           string    `json:"text"`
	TS             time.Time `json:"ts"`
}

type profileResponse struct {
	Username  string `json:"username"`
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
	AvatarHex string `json:"avatar_hex"`
	AvatarData string `json:"avatar_data"`
}

type profileUpdateRequest struct {
	FirstName string `json:"firstName,omitempty"`
	LastName  string `json:"lastName,omitempty"`
	AvatarHex string `json:"avatarHex,omitempty"`
	AvatarData string `json:"avatarData,omitempty"`
}

func registerHandler(authSvc *auth.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		var req credentials
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "bad request", http.StatusBadRequest)
			return
		}

		if req.AvatarHex == "" {
			req.AvatarHex = randomAvatarHex()
		}
		if err := authSvc.Register(req.Username, req.Password, req.FirstName, req.LastName, req.AvatarHex); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}

		w.WriteHeader(http.StatusCreated)
	}
}

func randomAvatarHex() string {
	const letters = "0123456789abcdef"
	b := make([]byte, 6)
	if _, err := rand.Read(b); err != nil {
		return "7a7a7a"
	}
	for i := range b {
		b[i] = letters[int(b[i])%len(letters)]
	}
	return string(b)
}

func loginHandler(authSvc *auth.Service) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		var req credentials
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "bad request", http.StatusBadRequest)
			return
		}

		token, err := authSvc.Login(req.Username, req.Password)
		if err != nil {
			http.Error(w, err.Error(), http.StatusUnauthorized)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(tokenResponse{Token: token})
	}
}

type deleteRequest struct {
	Token string `json:"token"`
}

func deleteHandler(authSvc *auth.Service, msgStore store.MessageStore, hub *ws.Hub) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		token := tokenFromRequest(r)
		if token == "" {
			http.Error(w, "missing token", http.StatusUnauthorized)
			return
		}

		username, ok := authSvc.UsernameForToken(token)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		if err := msgStore.DeleteUser(context.Background(), username); err != nil {
			http.Error(w, "failed to delete messages", http.StatusInternalServerError)
			return
		}

		if err := authSvc.DeleteAccount(context.Background(), username); err != nil {
			http.Error(w, "failed to delete user", http.StatusInternalServerError)
			return
		}

		authSvc.InvalidateToken(token)
		hub.RemoveUser(username)
		w.WriteHeader(http.StatusNoContent)
	}
}

func conversationsHandler(authSvc *auth.Service, msgStore store.MessageStore, userStore store.UserStore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		token := tokenFromRequest(r)
		if token == "" {
			http.Error(w, "missing token", http.StatusUnauthorized)
			return
		}

		username, ok := authSvc.UsernameForToken(token)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		items, err := msgStore.Conversations(context.Background(), username)
		if err != nil {
			http.Error(w, "failed to load conversations", http.StatusInternalServerError)
			return
		}

		peersSet := make(map[string]struct{})
		convPeers := make(map[string]string, len(items))
		for _, convID := range items {
			parts := strings.Split(convID, "|")
			if len(parts) != 2 {
				continue
			}
			peer := parts[0]
			if peer == username {
				peer = parts[1]
			}
			convPeers[convID] = peer
			peersSet[peer] = struct{}{}
		}

		var peers []string
		for peer := range peersSet {
			peers = append(peers, peer)
		}
		peersInfo, _ := userStore.GetMany(r.Context(), peers)

		out := make([]conversationItem, 0, len(items))
		for _, convID := range items {
			peer := convPeers[convID]
			info := peersInfo[peer]
			out = append(out, conversationItem{
				ConversationID: convID,
				PeerUsername:   peer,
				FirstName:      info.FirstName,
				LastName:       info.LastName,
			AvatarHex:      info.AvatarHex,
			AvatarData:     info.AvatarData,
		})
	}

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(conversationsResponse{Items: out})
	}
}

func usersSearchHandler(authSvc *auth.Service, userStore store.UserStore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		token := tokenFromRequest(r)
		if token == "" {
			http.Error(w, "missing token", http.StatusUnauthorized)
			return
		}

		if _, ok := authSvc.UsernameForToken(token); !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		query := r.URL.Query().Get("query")
		users, err := userStore.Search(r.Context(), query, 20)
		if err != nil {
			http.Error(w, "failed to search users", http.StatusBadRequest)
			return
		}

		out := make([]userDTO, 0, len(users))
		for _, user := range users {
			out = append(out, userDTO{
				Username:  user.Username,
				FirstName: user.FirstName,
				LastName:  user.LastName,
				AvatarHex: user.AvatarHex,
				AvatarData: user.AvatarData,
			})
		}

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(usersResponse{Items: out})
	}
}

func profileGetHandler(authSvc *auth.Service, userStore store.UserStore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		token := tokenFromRequest(r)
		if token == "" {
			http.Error(w, "missing token", http.StatusUnauthorized)
			return
		}

		username, ok := authSvc.UsernameForToken(token)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		target := r.URL.Query().Get("user")
		if target == "" {
			target = username
		}

		user, err := userStore.Get(r.Context(), target)
		if err != nil {
			http.Error(w, "profile not found", http.StatusNotFound)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(profileResponse{
			Username:  user.Username,
			FirstName: user.FirstName,
			LastName:  user.LastName,
			AvatarHex: user.AvatarHex,
			AvatarData: user.AvatarData,
		})
	}
}

func profileUpdateHandler(authSvc *auth.Service, userStore store.UserStore, hub *ws.Hub) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPatch {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		token := tokenFromRequest(r)
		if token == "" {
			http.Error(w, "missing token", http.StatusUnauthorized)
			return
		}

		username, ok := authSvc.UsernameForToken(token)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		var req profileUpdateRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "bad request", http.StatusBadRequest)
			return
		}

		user, err := userStore.UpdateProfile(r.Context(), username, req.FirstName, req.LastName, req.AvatarHex, req.AvatarData)
		if err != nil {
			http.Error(w, "failed to update profile", http.StatusBadRequest)
			return
		}

		hub.BroadcastProfile(ws.ProfileDTO{
			Username:   user.Username,
			FirstName:  user.FirstName,
			LastName:   user.LastName,
			AvatarHex:  user.AvatarHex,
			AvatarData: user.AvatarData,
		})

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(profileResponse{
			Username:  user.Username,
			FirstName: user.FirstName,
			LastName:  user.LastName,
			AvatarHex: user.AvatarHex,
			AvatarData: user.AvatarData,
		})
	}
}

func messagesSearchHandler(authSvc *auth.Service, msgStore store.MessageStore) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		token := tokenFromRequest(r)
		if token == "" {
			http.Error(w, "missing token", http.StatusUnauthorized)
			return
		}

		username, ok := authSvc.UsernameForToken(token)
		if !ok {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		query := r.URL.Query().Get("query")
		items, err := msgStore.SearchMessages(r.Context(), username, query, 50)
		if err != nil {
			http.Error(w, "failed to search messages", http.StatusBadRequest)
			return
		}

		out := make([]messageDTO, 0, len(items))
		for _, msg := range items {
			out = append(out, messageDTO{
				ID:             msg.ID,
				ConversationID: msg.ConversationID,
				From:           msg.From,
				To:             msg.To,
				Text:           msg.Text,
				TS:             msg.TS,
			})
		}

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(messagesResponse{Items: out})
	}
}

func tokenFromRequest(r *http.Request) string {
	authHeader := r.Header.Get("Authorization")
	if strings.HasPrefix(authHeader, "Bearer ") {
		return strings.TrimSpace(strings.TrimPrefix(authHeader, "Bearer "))
	}

	var req deleteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err == nil {
		return strings.TrimSpace(req.Token)
	}
	return ""
}

func initMessageStore() store.MessageStore {
	ctx := context.Background()
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		log.Println("DATABASE_URL is empty, using in-memory message store")
		return store.NewMemoryMessageStore()
	}

	pgStore, err := store.NewPostgresMessageStore(ctx, dbURL)
	if err != nil {
		log.Println("failed to init Postgres store, using in-memory:", err)
		return store.NewMemoryMessageStore()
	}

	return pgStore
}

func initUserStore() store.UserStore {
	ctx := context.Background()
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		log.Println("DATABASE_URL is empty, using in-memory user store")
		return store.NewMemoryUserStore()
	}

	pgStore, err := store.NewPostgresUserStore(ctx, dbURL)
	if err != nil {
		log.Println("failed to init Postgres user store, using in-memory:", err)
		return store.NewMemoryUserStore()
	}

	return pgStore
}
