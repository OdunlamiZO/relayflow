.PHONY: dev-infra stop-infra api-test plugins web-lint

dev-infra:
	docker compose up -d postgres redis

stop-infra:
	docker compose down

api-test:
	mvn test

# Builds the official plugins into apps/api/plugins, where a local API run loads them.
plugins:
	mvn -B -q -DskipTests install
	mkdir -p apps/api/plugins
	cp plugins/*/target/relayflow-plugin-*.jar apps/api/plugins/

web-lint:
	cd apps/web && npm run lint
