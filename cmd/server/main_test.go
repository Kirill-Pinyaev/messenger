package main

import (
	"context"
	"testing"
	"time"
)

func TestInitStoresUsesMemoryWhenDatabaseURLMissing(t *testing.T) {
	stores, err := initStoresWithRetry(context.Background(), "", 10*time.Millisecond, time.Millisecond)
	if err != nil {
		t.Fatalf("initStoresWithRetry() error = %v", err)
	}
	defer stores.close()

	if stores.userStore == nil || stores.msgStore == nil || stores.convStore == nil || stores.keyStore == nil || stores.archiveStore == nil {
		t.Fatalf("initStoresWithRetry() returned nil store: %+v", stores)
	}
}

func TestInitStoresReturnsErrorWhenConfiguredPostgresUnavailable(t *testing.T) {
	ctx := context.Background()
	_, err := initStoresWithRetry(ctx, "postgres://messenger:messenger@127.0.0.1:1/messenger?sslmode=disable", 10*time.Millisecond, time.Millisecond)
	if err == nil {
		t.Fatal("initStoresWithRetry() error = nil, want unavailable Postgres error")
	}
}
