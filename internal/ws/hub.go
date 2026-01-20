package ws

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"sort"
	"strings"
	"sync"
	"time"

	"messenger/internal/auth"
	"messenger/internal/store"

	"github.com/gorilla/websocket"
)

// Сообщение от клиента на сервер
// Пример:
// {"type":"dm","to":"bob","text":"привет"}
type ClientInMsg struct {
	Type           string `json:"type"`
	To             string `json:"to,omitempty"`
	Text           string `json:"text,omitempty"`
	ConversationID string `json:"conversation_id,omitempty"`
	With           string `json:"with,omitempty"`
	Limit          int    `json:"limit,omitempty"`
	MessageID      int64  `json:"message_id,omitempty"`
	Scope          string `json:"scope,omitempty"`
}

// Сообщение от сервера клиенту
// Пример:
// {"type":"dm","from":"alice","to":"bob","text":"привет","ts":"2026-01-20T13:00:00Z"}
type ServerOutMsg struct {
	Type           string       `json:"type"`
	ConversationID string       `json:"conversation_id,omitempty"`
	From           string       `json:"from,omitempty"`
	To             string       `json:"to,omitempty"`
	Text           string       `json:"text,omitempty"`
	TS             time.Time    `json:"ts"`
	Err            string       `json:"err,omitempty"`
	Users          []string     `json:"users,omitempty"`
	Messages       []MessageDTO `json:"messages,omitempty"`
	MessageID      int64        `json:"message_id,omitempty"`
	Profile        *ProfileDTO  `json:"profile,omitempty"`
}

type MessageDTO struct {
	ID             int64     `json:"message_id"`
	ConversationID string    `json:"conversation_id"`
	From           string    `json:"from"`
	To             string    `json:"to"`
	Text           string    `json:"text"`
	TS             time.Time `json:"ts"`
}

type ProfileDTO struct {
	Username   string `json:"username"`
	FirstName  string `json:"first_name"`
	LastName   string `json:"last_name"`
	AvatarHex  string `json:"avatar_hex"`
	AvatarData string `json:"avatar_data"`
}

type Client struct {
	username string
	conn     *websocket.Conn
	send     chan []byte
}

type Hub struct {
	mu      sync.RWMutex
	clients map[string]*Client
	auth    *auth.Service
	store   store.MessageStore
}

func NewHub(authSvc *auth.Service, msgStore store.MessageStore) *Hub {
	return &Hub{
		clients: make(map[string]*Client),
		auth:    authSvc,
		store:   msgStore,
	}
}

func (h *Hub) addClient(c *Client) error {
	h.mu.Lock()
	defer h.mu.Unlock()

	if c.username == "" {
		return errStr("empty username")
	}
	if _, exists := h.clients[c.username]; exists {
		return errStr("username already online")
	}

	h.clients[c.username] = c
	return nil
}

func (h *Hub) removeClient(username string) {
	var removed bool

	h.mu.Lock()
	if c, ok := h.clients[username]; ok {
		delete(h.clients, username)
		close(c.send)
		_ = c.conn.Close()
		removed = true
	}
	h.mu.Unlock()

	if removed {
		h.broadcastPresence()
	}
}

func (h *Hub) RemoveUser(username string) {
	h.removeClient(username)
}

func (h *Hub) BroadcastProfile(profile ProfileDTO) {
	msg := ServerOutMsg{
		Type:    "profile",
		Profile: &profile,
		TS:      time.Now().UTC(),
	}
	for _, c := range h.clientsSnapshot() {
		c.sendJSON(msg)
	}
}

func (h *Hub) getClient(username string) (*Client, bool) {
	h.mu.RLock()
	defer h.mu.RUnlock()
	c, ok := h.clients[username]
	return c, ok
}

func (h *Hub) clientsSnapshot() []*Client {
	h.mu.RLock()
	defer h.mu.RUnlock()

	out := make([]*Client, 0, len(h.clients))
	for _, c := range h.clients {
		out = append(out, c)
	}
	return out
}

func (h *Hub) presenceSnapshot() []string {
	h.mu.RLock()
	defer h.mu.RUnlock()

	users := make([]string, 0, len(h.clients))
	for username := range h.clients {
		users = append(users, username)
	}
	sort.Strings(users)
	return users
}

func (h *Hub) broadcastPresence() {
	users := h.presenceSnapshot()
	msg := ServerOutMsg{
		Type:  "presence",
		Users: users,
		TS:    time.Now().UTC(),
	}
	for _, c := range h.clientsSnapshot() {
		c.sendJSON(msg)
	}
}

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		// Для разработки: разрешаем все источники.
		// Потом ужесточим.
		return true
	},
}

func ServeWS(hub *Hub, w http.ResponseWriter, r *http.Request) {
	token := r.URL.Query().Get("token")
	username, ok := hub.auth.UsernameForToken(token)
	if !ok {
		http.Error(w, "unauthorized", http.StatusUnauthorized)
		return
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("ws upgrade error:", err)
		return
	}

	c := &Client{
		username: username,
		conn:     conn,
		send:     make(chan []byte, 32),
	}

	if err := hub.addClient(c); err != nil {
		// Пытаемся отправить ошибку и закрыть соединение
		_ = conn.WriteJSON(ServerOutMsg{
			Type: "error",
			Err:  err.Error(),
			TS:   time.Now().UTC(),
		})
		_ = conn.Close()
		return
	}

	hub.broadcastPresence()

	// Уведомление самому себе (удобно для UI)
	_ = conn.WriteJSON(ServerOutMsg{
		Type: "info",
		Text: "connected as " + username,
		TS:   time.Now().UTC(),
	})

	go c.writePump()
	c.readPump(hub)
}

func (c *Client) readPump(hub *Hub) {
	defer hub.removeClient(c.username)

	_ = c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	c.conn.SetPongHandler(func(string) error {
		_ = c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})

	for {
		_, data, err := c.conn.ReadMessage()
		if err != nil {
			return
		}

		var in ClientInMsg
		if err := json.Unmarshal(data, &in); err != nil {
			c.sendJSON(ServerOutMsg{
				Type: "error",
				Err:  "bad json",
				TS:   time.Now().UTC(),
			})
			continue
		}

		switch in.Type {
		case "dm":
			c.handleDM(hub, in)
		case "history":
			c.handleHistory(hub, in)
		case "delete":
			c.handleDelete(hub, in)
		default:
			c.sendJSON(ServerOutMsg{
				Type: "error",
				Err:  "unknown type",
				TS:   time.Now().UTC(),
			})
		}
	}
}

func (c *Client) handleDM(hub *Hub, in ClientInMsg) {
	now := time.Now().UTC()

	if in.To == "" || in.Text == "" {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "to and text are required",
			TS:   now,
		})
		return
	}

	convID := conversationID(c.username, in.To)

	saved, err := hub.store.Save(context.Background(), store.Message{
		ConversationID: convID,
		From:           c.username,
		To:             in.To,
		Text:           in.Text,
		TS:             now,
	})
	if err != nil {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "save error",
			TS:   time.Now().UTC(),
		})
		return
	}

	// Сообщение адресату
	dstMsg := ServerOutMsg{
		Type:           "dm",
		MessageID:      saved.ID,
		ConversationID: convID,
		From:           c.username,
		To:             in.To,
		Text:           in.Text,
		TS:             now,
	}
	if dst, ok := hub.getClient(in.To); ok {
		dst.sendJSON(dstMsg)
		c.sendJSON(ServerOutMsg{
			Type:      "delivered",
			MessageID: saved.ID,
			TS:        now,
		})
	}

	// Эхо отправителю (чтобы видеть что отправилось)
	selfMsg := ServerOutMsg{
		Type:           "dm",
		MessageID:      saved.ID,
		ConversationID: convID,
		From:           c.username,
		To:             in.To,
		Text:           in.Text,
		TS:             now,
	}
	c.sendJSON(selfMsg)
}

func (c *Client) handleHistory(hub *Hub, in ClientInMsg) {
	convID := strings.TrimSpace(in.ConversationID)
	if convID == "" && in.With != "" {
		convID = conversationID(c.username, strings.TrimSpace(in.With))
	}
	if convID == "" {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "conversation_id or with is required",
			TS:   time.Now().UTC(),
		})
		return
	}

	msgs, err := hub.store.History(context.Background(), convID, in.Limit)
	if err != nil {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "history error",
			TS:   time.Now().UTC(),
		})
		return
	}

	out := make([]MessageDTO, 0, len(msgs))
	for _, msg := range msgs {
		out = append(out, MessageDTO{
			ID:             msg.ID,
			ConversationID: msg.ConversationID,
			From:           msg.From,
			To:             msg.To,
			Text:           msg.Text,
			TS:             msg.TS,
		})
	}

	c.sendJSON(ServerOutMsg{
		Type:           "history",
		ConversationID: convID,
		Messages:       out,
		TS:             time.Now().UTC(),
	})
}

func (c *Client) handleDelete(hub *Hub, in ClientInMsg) {
	if in.MessageID <= 0 || in.Scope != "all" {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "bad delete request",
			TS:   time.Now().UTC(),
		})
		return
	}

	msg, err := hub.store.GetByID(context.Background(), in.MessageID)
	if err != nil {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "message not found",
			TS:   time.Now().UTC(),
		})
		return
	}
	if msg.From != c.username {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "not allowed",
			TS:   time.Now().UTC(),
		})
		return
	}

	if err := hub.store.DeleteMessage(context.Background(), in.MessageID); err != nil {
		c.sendJSON(ServerOutMsg{
			Type: "error",
			Err:  "delete failed",
			TS:   time.Now().UTC(),
		})
		return
	}

	out := ServerOutMsg{
		Type:           "deleted",
		MessageID:      in.MessageID,
		ConversationID: msg.ConversationID,
		TS:             time.Now().UTC(),
	}

	if dst, ok := hub.getClient(msg.To); ok {
		dst.sendJSON(out)
	}
	c.sendJSON(out)
}

func (c *Client) writePump() {
	ticker := time.NewTicker(25 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case msg, ok := <-c.send:
			if !ok {
				_ = c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := c.conn.WriteMessage(websocket.TextMessage, msg); err != nil {
				return
			}
		case <-ticker.C:
			// ping, чтобы соединение не отваливалось молча
			_ = c.conn.WriteMessage(websocket.PingMessage, nil)
		}
	}
}

func (c *Client) sendJSON(v any) {
	b, err := json.Marshal(v)
	if err != nil {
		return
	}
	select {
	case c.send <- b:
	default:
		// если клиент не читает — считаем его “плохим” и закрываем
		_ = c.conn.Close()
	}
}

func conversationID(a, b string) string {
	if strings.Compare(a, b) <= 0 {
		return a + "|" + b
	}
	return b + "|" + a
}

type errStr string

func (e errStr) Error() string { return string(e) }
