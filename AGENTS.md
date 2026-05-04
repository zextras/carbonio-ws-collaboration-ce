# AGENTS.md — carbonio-ws-collaboration

## Build

- `mvn clean install` — full build (multi-module)
- `mvn clean install -DskipTests` — skip tests
- `mvn test` — unit tests only (carbonio-ws-collaboration-core)
- `mvn verify` — unit + integration tests (carbonio-ws-collaboration-it)

## Modules

- `carbonio-ws-collaboration-boot` — entry point, Guice wiring
- `carbonio-ws-collaboration-core` — business logic, domain, infrastructure
- `carbonio-ws-collaboration-it` — integration tests (TestContainers)
- `carbonio-ws-collaboration-openapi` — OpenAPI spec + generated REST server stubs

## Framework

- Guice + Jetty + RestEasy (NOT Quarkus). CDI annotations do NOT apply.
- Dependency injection: Guice `@Provides` methods in `CoreModule.java`
- All new infrastructure clients must be registered in `CoreModule.java`

## Package / Deploy

- `package/PKGBUILD` — package spec
- `./check_integrity.sh package/PKGBUILD` — verify hashes before committing
- Consul files: `package/policies.json`, `package/intentions.json`
- `consul.sh` — local dev helper for consul setup

## Conventions

- License: AGPL-3.0-only; new files need SPDX header
- REUSE.toml covers license compliance; update if adding new directories
- Config keys live in `ConfigName.java`; Consul KV mappings in `ConsulAppConfig.java`
- gRPC clients (after videorecorder migration): use `ManagedChannelBuilder`, inject via Guice, no CDI
