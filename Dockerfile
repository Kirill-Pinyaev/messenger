FROM node:22-alpine AS webbuild

WORKDIR /src
RUN apk add --no-cache unzip curl
COPY api ./api
COPY scripts ./scripts
COPY web ./web
RUN chmod +x /src/scripts/protoc.sh
WORKDIR /src/web
RUN npm ci
RUN npm run build

FROM golang:1.25-alpine AS build

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download

COPY api ./api
COPY cmd ./cmd
COPY internal ./internal
COPY gen ./gen
RUN go build -o /out/server ./cmd/server

FROM alpine:3.19
WORKDIR /app
COPY --from=build /out/server /app/server
COPY --from=webbuild /src/web/dist /app/web/dist
EXPOSE 8082
EXPOSE 9090
CMD ["/app/server"]
