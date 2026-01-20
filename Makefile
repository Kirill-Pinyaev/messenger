.PHONY: up down restart logs db-clean

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
