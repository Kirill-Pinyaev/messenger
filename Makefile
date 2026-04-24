GOPATH := $(shell go env GOPATH)
PROTOC ?= ./scripts/protoc.sh
PROTOC_INCLUDE ?= ./.tools/protoc-29.3/include

.PHONY: up down restart logs db-clean proto test test-integration test-web cover cover-html

up:
	docker compose up -d --build

down:
	docker compose down

restart:
	docker compose down
	docker compose up -d --build

logs:
	docker compose logs -f --tail=200

db-clean:
	docker compose down -v

proto:
	PATH=$$PATH:$(GOPATH)/bin $(PROTOC) -I $(PROTOC_INCLUDE) -I api/proto \
		--go_out=paths=source_relative:gen \
		--go-grpc_out=paths=source_relative:gen \
		api/proto/messenger/v1/messenger.proto

test:
	go test ./...

test-integration:
	go test -tags=integration ./internal/store -run TestPostgresStoresIntegration -count=1

test-web:
	cd web && node --test src/lib/*.test.js

cover:
	go test ./internal/auth ./internal/grpcapi ./internal/store -coverprofile=coverage.out
	go tool cover -func=coverage.out

cover-html:
	go test ./internal/auth ./internal/grpcapi ./internal/store -coverprofile=coverage.out
	go tool cover -html=coverage.out
