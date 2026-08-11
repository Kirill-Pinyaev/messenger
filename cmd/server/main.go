package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"time"

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

	userStore, msgStore, convStore, keyStore, archiveStore, closePool := initStores(ctx)
	defer closePool()

	authSvc := auth.NewService(userStore)
	apiServer := grpcapi.NewServer(authSvc, userStore, msgStore, convStore, keyStore, archiveStore)

	grpcAddr := envOrDefault("GRPC_ADDR", ":9090")
	httpAddr := envOrDefault("HTTP_ADDR", ":8082")

	grpcServer := newGRPCServer(apiServer)
	go serveHTTP(httpAddr, grpcServer)
	serveGRPC(grpcAddr, grpcServer)
}

func initStores(ctx context.Context) (store.UserStore, store.MessageStore, store.ConversationStore, store.KeyStore, store.ArchiveStore, func()) {
	dbURL := os.Getenv("DATABASE_URL")
	timeout := envDurationOrDefault("POSTGRES_CONNECT_TIMEOUT", 30*time.Second)
	stores, err := initStoresWithRetry(ctx, dbURL, timeout, time.Second)
	if err != nil {
		log.Fatal(err)
	}
	return stores.userStore, stores.msgStore, stores.convStore, stores.keyStore, stores.archiveStore, stores.close
}

type initializedStores struct {
	userStore    store.UserStore
	msgStore     store.MessageStore
	convStore    store.ConversationStore
	keyStore     store.KeyStore
	archiveStore store.ArchiveStore
	close        func()
}

func initStoresWithRetry(ctx context.Context, dbURL string, timeout, interval time.Duration) (initializedStores, error) {
	if dbURL == "" {
		log.Println("DATABASE_URL is empty, using in-memory stores")
		return initializedStores{
			userStore:    store.NewMemoryUserStore(),
			msgStore:     store.NewMemoryMessageStore(),
			convStore:    store.NewMemoryConversationStore(),
			keyStore:     store.NewMemoryKeyStore(),
			archiveStore: store.NewMemoryArchiveStore(),
			close:        func() {},
		}, nil
	}

	if timeout <= 0 {
		timeout = 30 * time.Second
	}
	if interval <= 0 {
		interval = time.Second
	}
	deadline := time.Now().Add(timeout)
	var lastErr error
	for {
		stores, err := initPostgresStores(ctx, dbURL)
		if err == nil {
			log.Println("connected to Postgres")
			return stores, nil
		}
		lastErr = err
		if time.Now().After(deadline) {
			return initializedStores{}, fmt.Errorf("failed to initialize Postgres stores after %s: %w", timeout, lastErr)
		}
		log.Println("Postgres is not ready yet, retrying:", err)
		timer := time.NewTimer(interval)
		select {
		case <-ctx.Done():
			timer.Stop()
			return initializedStores{}, ctx.Err()
		case <-timer.C:
		}
	}
}

func initPostgresStores(ctx context.Context, dbURL string) (initializedStores, error) {
	pool, err := pgxpool.New(ctx, dbURL)
	if err != nil {
		return initializedStores{}, err
	}

	userStore, err := store.NewPostgresUserStore(ctx, pool)
	if err != nil {
		pool.Close()
		return initializedStores{}, err
	}

	msgStore, err := store.NewPostgresMessageStore(ctx, pool)
	if err != nil {
		pool.Close()
		return initializedStores{}, err
	}

	convStore, err := store.NewPostgresConversationStore(ctx, pool)
	if err != nil {
		pool.Close()
		return initializedStores{}, err
	}

	keyStore, err := store.NewPostgresKeyStore(ctx, pool)
	if err != nil {
		pool.Close()
		return initializedStores{}, err
	}
	archiveStore, err := store.NewPostgresArchiveStore(ctx, pool)
	if err != nil {
		pool.Close()
		return initializedStores{}, err
	}

	return initializedStores{
		userStore:    userStore,
		msgStore:     msgStore,
		convStore:    convStore,
		keyStore:     keyStore,
		archiveStore: archiveStore,
		close:        pool.Close,
	}, nil
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

func envDurationOrDefault(key string, fallback time.Duration) time.Duration {
	if value := os.Getenv(key); value != "" {
		parsed, err := time.ParseDuration(value)
		if err != nil {
			log.Fatal(err)
		}
		return parsed
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
