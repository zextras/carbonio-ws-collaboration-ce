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

## Library publishing

- `openapi` and `core` are published as Maven jars, `it` as a test-jar (`carbonio-ws-collaboration-ce-*`, version
  `${revision}${changelist}`) and consumed by downstream projects that subclass CE classes, override protected
  hooks and extend `ExtensionModule`. Keep public classes, protected hooks and extension modules backward compatible.
- Meeting-scoped Janus calls in `VideoServerServiceImpl` must go through `clientFor` / `clientForNewMeeting`,
  never the injected client field directly (only `isAlive()` may); `VideoServerServiceImplTest` enforces this.

## Framework

- Guice + Jetty + RestEasy (NOT Quarkus). CDI annotations do NOT apply.
- Dependency injection: Guice modules in `core/config/module/`. `CoreModule` installs the always-on
  modules (`WebModule`, `MessagingModule`, `UserModule`, `RoomModule`, `ChatModule`); `ExtensionModule`
  installs the modules a downstream build may replace or extend (`DatabaseModule`, `AttachmentModule`,
  `MeetingModule`, `WebSocketModule`). `BootModule` installs both, plus `ProductionConfig` +
  `ConsulDiscoveryModule` (or `DockerConfig` + `DockerDiscoveryModule` when `APP_ENVIRONMENT=docker`).
  `@Provides` methods live in the module that owns the feature.
- `MessageSentListener` (multibound in `ChatModule`, empty by default) is called by
  `RoomsApiServiceImpl.sendMessage` and `RoomsApiServiceImpl.insertAttachmentMultipart` (text = caption or
  `""`) after a message is stored and broadcast; downstream builds add listeners with
  `Multibinder.newSetBinder(binder(), MessageSentListener.class).addBinding()`.
- **New classes**: annotate with `@com.google.inject.Singleton` (when stateful/shared), constructor-inject
  dependencies with `@Inject`, never instantiate with `new` in production code, and add an explicit
  `bind(NewClass.class);` in the module that owns the feature (project convention, even though Guice can JIT-bind).
  Tests that build the class directly must be updated to pass the new dependency.
- All new infrastructure clients must be registered in the module of the feature that uses them.

## Package / Deploy

- `package/PKGBUILD` — package spec
- `./check_integrity.sh package/PKGBUILD` — verify hashes before committing
- Consul files: `package/policies.json`, `package/intentions.json`
- `consul.sh` — local dev helper for consul setup

## Conventions

- License: AGPL-3.0-only; new files need SPDX header
- REUSE.toml covers license compliance; update if adding new directories
- Config keys live in `ConfigName.java`; Consul KV mappings, environment keys and infrastructure defaults
  live in `CoreConfigContribution.java` (a `ConfigContribution`, multibound in `WebModule`). Duplicate keys are rejected when
  the config sources are built (env keys in docker mode; infrastructure defaults and Consul mappings in production mode).
- Flyway migrations live in `classpath:migration/ce` (`DatabaseModule.migrationsLocation()`)
- gRPC clients (after videorecorder migration): use `ManagedChannelBuilder`, inject via Guice, no CDI
