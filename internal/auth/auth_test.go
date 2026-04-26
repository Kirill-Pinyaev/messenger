package auth

import (
	"context"
	"errors"
	"testing"

	"messenger/internal/store"
)

func TestServiceRegisterLoginDeleteAccount(t *testing.T) {
	t.Parallel()

	userStore := store.NewMemoryUserStore()
	svc := NewService(userStore)

	if err := svc.Register(" alice ", "secret", "Alice", "Doe", "112233", ""); err != nil {
		t.Fatalf("Register() error = %v", err)
	}

	user, err := userStore.Get(context.Background(), "alice")
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if user.Username != "alice" {
		t.Fatalf("username = %q, want %q", user.Username, "alice")
	}
	if user.FirstName != "Alice" || user.LastName != "Doe" {
		t.Fatalf("unexpected profile data: %+v", user)
	}
	if len(user.Salt) == 0 || len(user.Hash) == 0 {
		t.Fatalf("salt/hash must be populated: %+v", user)
	}

	token, err := svc.Login("alice", "secret", "web-1")
	if err != nil {
		t.Fatalf("Login() error = %v", err)
	}
	if token == "" {
		t.Fatal("Login() returned empty token")
	}

	session, ok := svc.SessionForToken(token)
	if !ok || session.Username != "alice" || session.DeviceID != "web-1" {
		t.Fatalf("SessionForToken() = (%+v, %v), want username=%q device=%q", session, ok, "alice", "web-1")
	}

	if err := svc.DeleteAccount(context.Background(), "alice"); err != nil {
		t.Fatalf("DeleteAccount() error = %v", err)
	}
	if _, err := userStore.Get(context.Background(), "alice"); !errors.Is(err, store.ErrUserNotFound) {
		t.Fatalf("Get() after delete error = %v, want %v", err, store.ErrUserNotFound)
	}
	if _, ok := svc.SessionForToken(token); ok {
		t.Fatal("token should be invalidated after DeleteAccount()")
	}
}

func TestServiceRegisterRejectsDuplicateUser(t *testing.T) {
	t.Parallel()

	svc := NewService(store.NewMemoryUserStore())

	if err := svc.Register("alice", "secret", "", "", "", ""); err != nil {
		t.Fatalf("first Register() error = %v", err)
	}
	err := svc.Register("alice", "secret", "", "", "", "")
	if !errors.Is(err, ErrUserExists) {
		t.Fatalf("second Register() error = %v, want %v", err, ErrUserExists)
	}
}

func TestServiceLoginRejectsInvalidCredentials(t *testing.T) {
	t.Parallel()

	svc := NewService(store.NewMemoryUserStore())
	if err := svc.Register("alice", "secret", "", "", "", ""); err != nil {
		t.Fatalf("Register() error = %v", err)
	}

	tests := []struct {
		name     string
		username string
		password string
		wantErr  error
	}{
		{name: "wrong password", username: "alice", password: "bad", wantErr: ErrWrongPassword},
		{name: "missing user", username: "bob", password: "secret", wantErr: ErrUserNotFound},
		{name: "empty username", username: "", password: "secret", wantErr: ErrBadInput},
		{name: "empty password", username: "alice", password: "", wantErr: ErrBadInput},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			t.Parallel()

			_, err := svc.Login(tt.username, tt.password, "web-1")
			if !errors.Is(err, tt.wantErr) {
				t.Fatalf("Login() error = %v, want %v", err, tt.wantErr)
			}
		})
	}
}

func TestServiceInvalidateToken(t *testing.T) {
	t.Parallel()

	svc := NewService(store.NewMemoryUserStore())
	if err := svc.Register("alice", "secret", "", "", "", ""); err != nil {
		t.Fatalf("Register() error = %v", err)
	}

	token, err := svc.Login("alice", "secret", "web-1")
	if err != nil {
		t.Fatalf("Login() error = %v", err)
	}

	svc.InvalidateToken(token)
	if _, ok := svc.SessionForToken(token); ok {
		t.Fatal("token should be invalid after InvalidateToken()")
	}
}

func TestServiceLoginRequiresDeviceID(t *testing.T) {
	t.Parallel()

	svc := NewService(store.NewMemoryUserStore())
	if err := svc.Register("alice", "secret", "", "", "", ""); err != nil {
		t.Fatalf("Register() error = %v", err)
	}

	if _, err := svc.Login("alice", "secret", ""); !errors.Is(err, ErrBadInput) {
		t.Fatalf("Login() error = %v, want %v", err, ErrBadInput)
	}
}
