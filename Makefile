.PHONY: dev-infra stop-infra api-test web-lint

dev-infra:
	docker compose up -d postgres redis

stop-infra:
	docker compose down

api-test:
	cd apps/api && mvn test

web-lint:
	cd apps/web && npm run lint
