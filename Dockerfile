FROM golang:1.22-alpine AS build

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download

COPY cmd ./cmd
COPY internal ./internal
COPY web ./web
RUN go build -o /out/server ./cmd/server

FROM alpine:3.19
WORKDIR /app
COPY --from=build /out/server /app/server
COPY --from=build /src/web /app/web
EXPOSE 8082
CMD ["/app/server"]
