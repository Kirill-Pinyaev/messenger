package main

import (
	"context"
	"log"
	"net"
	"net/http"
	"os"
	"path/filepath"

	messengerv1 "messenger/gen/messenger/v1"
	"messenger/internal/auth"
	"messenger/internal/grpcapi"
	"messenger/internal/store"

	"github.com/improbable-eng/grpc-web/go/grpcweb"
	"github.com/jackc/pgx/v5/pgxpool"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	ctx := context.Background()

	userStore, msgStore, convStore, keyStore, closePool := initStores(ctx)
	defer closePool()

	authSvc := auth.NewService(userStore)
	apiServer := grpcapi.NewServer(authSvc, userStore, msgStore, convStore, keyStore)

	grpcAddr := envOrDefault("GRPC_ADDR", ":9090")
	httpAddr := envOrDefault("HTTP_ADDR", ":8082")

	grpcServer := newGRPCServer(apiServer)
	go serveHTTP(httpAddr, grpcServer)
	serveGRPC(grpcAddr, grpcServer)
}

func initStores(ctx context.Context) (store.UserStore, store.MessageStore, store.ConversationStore, store.KeyStore, func()) {
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		log.Println("DATABASE_URL is empty, using in-memory stores")
		return store.NewMemoryUserStore(), store.NewMemoryMessageStore(), store.NewMemoryConversationStore(), store.NewMemoryKeyStore(), func() {}
	}

	pool, err := pgxpool.New(ctx, dbURL)
	if err != nil {
		log.Println("failed to connect to Postgres, using in-memory stores:", err)
		return store.NewMemoryUserStore(), store.NewMemoryMessageStore(), store.NewMemoryConversationStore(), store.NewMemoryKeyStore(), func() {}
	}

	userStore, err := store.NewPostgresUserStore(ctx, pool)
	if err != nil {
		pool.Close()
		log.Println("failed to init Postgres user store, using in-memory stores:", err)
		return store.NewMemoryUserStore(), store.NewMemoryMessageStore(), store.NewMemoryConversationStore(), store.NewMemoryKeyStore(), func() {}
	}

	msgStore, err := store.NewPostgresMessageStore(ctx, pool)
	if err != nil {
		pool.Close()
		log.Println("failed to init Postgres message store, using in-memory stores:", err)
		return store.NewMemoryUserStore(), store.NewMemoryMessageStore(), store.NewMemoryConversationStore(), store.NewMemoryKeyStore(), func() {}
	}

	convStore, err := store.NewPostgresConversationStore(ctx, pool)
	if err != nil {
		pool.Close()
		log.Println("failed to init Postgres conversation store, using in-memory stores:", err)
		return store.NewMemoryUserStore(), store.NewMemoryMessageStore(), store.NewMemoryConversationStore(), store.NewMemoryKeyStore(), func() {}
	}

	keyStore, err := store.NewPostgresKeyStore(ctx, pool)
	if err != nil {
		pool.Close()
		log.Println("failed to init Postgres key store, using in-memory stores:", err)
		return store.NewMemoryUserStore(), store.NewMemoryMessageStore(), store.NewMemoryConversationStore(), store.NewMemoryKeyStore(), func() {}
	}

	log.Println("connected to Postgres")
	return userStore, msgStore, convStore, keyStore, pool.Close
}

func serveHTTP(addr string, grpcServer *grpc.Server) {
	wrapped := grpcweb.WrapServer(
		grpcServer,
		grpcweb.WithOriginFunc(func(origin string) bool { return true }),
	)

	mux := http.NewServeMux()
	webDir := staticDir()
	fileServer := http.FileServer(http.Dir(webDir))
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if wrapped.IsGrpcWebRequest(r) || wrapped.IsGrpcWebSocketRequest(r) || wrapped.IsAcceptableGrpcCorsRequest(r) {
			wrapped.ServeHTTP(w, r)
			return
		}
		fileServer.ServeHTTP(w, r)
	})

	log.Println("http + grpc-web listening on", addr)
	log.Println("open:", "http://localhost"+addr+"/")
	if err := http.ListenAndServe(addr, mux); err != nil {
		log.Fatal(err)
	}
}

func serveGRPC(addr string, grpcServer *grpc.Server) {
	lis, err := net.Listen("tcp", addr)
	if err != nil {
		log.Fatal(err)
	}

	log.Println("gRPC listening on", addr)
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatal(err)
	}
}

func newGRPCServer(apiServer *grpcapi.Server) *grpc.Server {
	server := grpc.NewServer()
	messengerv1.RegisterAuthServiceServer(server, apiServer)
	messengerv1.RegisterUserServiceServer(server, apiServer)
	messengerv1.RegisterMessageServiceServer(server, apiServer)
	reflection.Register(server)
	return server
}

func envOrDefault(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func staticDir() string {
	distDir := filepath.Join(".", "web", "dist")
	if info, err := os.Stat(distDir); err == nil && info.IsDir() {
		return distDir
	}
	return filepath.Join(".", "web")
}
