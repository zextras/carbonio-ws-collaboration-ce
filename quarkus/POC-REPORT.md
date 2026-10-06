<!--
SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>

SPDX-License-Identifier: AGPL-3.0-only
-->

# Quarkus plugin PoC — report

Branch `poc/quarkus-plugins` (from `feat/jar-dependency-poc`, merge base `e2a92e1c`, 18 commits). Everything lives in `quarkus/`;
the root Guice build is untouched (`git diff --stat feat/jar-dependency-poc -- . ':(exclude)quarkus' ':(exclude)REUSE.toml'`
prints nothing). Last full run of `mvn -f quarkus/pom.xml clean verify`: BUILD SUCCESS in 1:05 min.

## What was built

Maven tree `quarkus/` (Quarkus 3.38.3, Java 21), not part of the root modules. Run everything with `mvn -f quarkus/pom.xml verify`
(Docker required: Testcontainers Postgres/RabbitMQ).

| Module | Content |
|---|---|
| `core-api` | Plugin contract: `PluginApi`/`PluginDescriptor`, `UserPrincipal`, `MeetingView`, `MeetingAccess`, `VideoServerGateway`, `EventPublisher`, `ServiceDiscovery`, `MeetingDtoEnricher`, `PluginMigrations`, `ConsulKeyMapping`, CDI events, async event models generated from `asyncapi.yaml`. Jandex-indexed, no Quarkus implementation. 8 tests. |
| `core` | Ported CE slice: Panache persistence on the CE schema (12 migrations), `ZM_AUTH_TOKEN` auth via the user-management SDK, RabbitMQ events, Janus client + videoserver service, meeting lifecycle REST API, `/events` websocket, plugin registry (API version check, per-plugin Flyway), Consul KV config source. 67 tests. |
| `test-support` | Shared test helpers (Seed, TestUsers, Stubs, WireMock resource, rabbitmq-mock producer). |
| `plugin-turn` | TURN feature as a plugin (A): client, service, `GET /meetings/{id}/turnCredentials`. Depends on `core-api` only. 9 tests. |
| `plugin-recording` | Recording feature as a plugin (B1): entity/repository, videorecorder client, Janus DTOs, start/stop endpoints, `MeetingStopping` observer, enricher, V1 migration. Shaded with the videorecorder SDK only. 18 tests. |
| `plugin-incompatible` (+ `test-plugins`) | Plugin declaring `requiredApiVersion` 2.0, used for the fail-fast test. |
| `app-ce` | Application = `core`, `mutable-jar`, `quarkus.package.jar.user-providers-directory=plugins`. 2 tests (plugin absent). |
| `app-adv` | Application = `core` + `plugin-turn`, standard fast-jar. 8 tests. |
| `distribution-it` | Failsafe IT driving the B1 cycle with external processes on the real `app-ce` distribution. 4 tests. |

Total: about 116 automated tests across the tree, all green.

## Plugin contract

| Guice seam (CE/ADV today) | `core-api` replacement as implemented | Fit |
|---|---|---|
| `stopActiveRecording` hook | Synchronous CDI event `MeetingStopping(meetingId, userId)`, fired before `videoServer.stopMeeting` | Fits. Fired from stop, delete and last-leave. |
| `afterMeetingStarted` | CDI event `MeetingStarted` | Fits (no plugin needs it yet). |
| `MeetingMapperImpl.enrich` | `MeetingDtoEnricher` collected via `Instance<>`; `MeetingDto.extras` with `@JsonAnyGetter` | Fits: `recStartedAt`/`recUserId` stay top-level. Not documented in openapi. |
| `VideoServerServiceImpl` plugin-message helpers, `getMeetingServerId` | `VideoServerGateway` (`serverIdOf`, `sendVideoRoomMessage`, `sendAudioBridgeMessage`) | Fits: plugin owns its Janus DTOs. |
| Recording validation in the advanced service | `MeetingAccess.requireActiveMeetingModerator` → `MeetingView` | Fits. |
| `eventDispatcher.sendToUserExchange` | `EventPublisher.sendToUsers` | Fits. |
| `discoveryService.isServiceAvailable` | `ServiceDiscovery.isAvailable` | Fits. |
| `Multibinder<ConfigContribution>` | MicroProfile Config + `ConsulKeyMapping` ServiceLoader file contributed by the plugin | Fits, but needs the extra ServiceLoader file (see Problems). |
| Separate `migration/advanced` Flyway series | `PluginMigrations(name, location)`; one Flyway per plugin, history table `flyway_history_<plugin>`, run in a `StartupEvent` observer after core migrations | Fits: FKs to `CHATS.MEETING` valid. Duplicate ids/locations are not validated. |
| — | `PluginDescriptor` + `requiredApiVersion`; fail fast on incompatibility | New. Essential for B1. |

What did not fit cleanly: the `ConfigSourceFactory` runs at augmentation too (it needed explicit sysprop/env handling for Consul
settings); `StoppingProbe`-style test beans must be `@Singleton`, not `@ApplicationScoped` (client proxies hide public fields); the
403 message of `ForbiddenException(message)` is not rendered in the body by Quarkus.

## A — build-time assembly (TURN)

What worked: `plugin-turn` depends only on `core-api`; `app-adv` adds it as a Maven dependency and the standard fast-jar build
discovers its beans, resource and config through Jandex. No runtime cost beyond the plugin's own classes.

Evidence: `app-adv` `TurnCredentialsTest` (7) covers 200 with a mock turn server, 204 when `carbonio-turn-server-auth` is not
available, 404 on missing serverId / `TurnServerException`, fallback to `getCredentialsAny` on 503/404/0; `TurnUnavailableTest`
(1); `plugin-turn` unit tests (9). In `app-ce` the same endpoint is absent (`PluginAbsentTest`, 2).

Cost: `app-adv` fast-jar is 44 MB (`du -sh quarkus/app-adv/target/quarkus-app`), versus 85 MB for the `app-ce` mutable-jar
(which also carries `lib/deployment`, 42 MB). Adding or removing a plugin means rebuilding the application artifact.

## B1 — re-augmentation (recording)

Operator flow: copy `plugin-recording-*-plugin.jar` into `quarkus-app/plugins/`, run
`java -Dquarkus.launch.rebuild=true -jar quarkus-run.jar`, restart the application normally.

Measurements (fresh run, `distribution-it/target/b1-metrics.properties`; single run, whole MB, `dist.size.mb` measured before
re-augmentation, warm cache, no Consul):

| Metric | Value |
|---|---|
| `reaugment.seconds` | 1.809 |
| `startup.ce.seconds` | 1.425 |
| `startup.recording.seconds` | 1.406 |
| `dist.size.mb` | 84 (du: 85 MB) |
| `deployment.size.mb` | 41 (du: 42 MB `lib/deployment`) |
| plugin jar (shaded) | 81,428 bytes (about 80 KB) |

Evidence (`RecordingPluginB1IT`, 4 tests, all green):
1. CE distribution: `/plugins` is empty, `startRecording` returns 404, no `chats.recording` table.
2. Jar dropped but not rebuilt: ignored. `/plugins` still empty, no table.
3. After re-augmentation and restart: log line `Loaded plugin recording 0.1.0 (API 1.0)`, `flyway_history_recording` and
   `chats.recording` created, `startRecording` 204 with the Janus mock receiving `enable_recording`, `GET /meetings/{id}`
   exposes `recStartedAt`, stopping the meeting leaves the recording STOPPED and calls the videorecorder mock with the
   `service-id` header.
4. Incompatible plugin (`requiredApiVersion` 2.0): the process exits non-zero with
   `Plugin 'incompatible' 0.1.0 requires plugin API 2.0 but core provides 1.0`.

## Problems found and how they were solved

Every deviation and fix recorded during Tasks 1–12:

- T1: Quarkus BOM does not manage `assertj-core` (added 3.27.3 in the parent); generated async models need
  `jackson-databind-nullable` (0.2.10, same as root); surefire's `java.util.logging.manager` setting prints a stack trace in
  non-Quarkus modules, solved by test-scope `jboss-logmanager` in `core-api` (and `plugin-turn`).
- T2: the forked `@QuarkusTest` bootstrap walks up to the repo-root pom, whose version uses a `changelist` defined only in a
  profile; solved with `quarkus/.mvn/maven.config` (`-Dchangelist=-SNAPSHOT`, covered in `REUSE.toml`). Flyway adds a schema
  creation history row, so the migration-count test filters `type = 'SQL'`.
- T3: `quarkus-security` is not transitive from `quarkus-rest-jackson`, added to `core`; the user-management SDK sends the token
  as a `ZM_AUTH_TOKEN` header (not a cookie) so stubs match on header; `CurrentUserImpl.get()` throws `ForbiddenException` when a
  denied reason is set (defense in depth, found in review).
- T4: serialized event `type` values are the generated `EventType` values in PascalCase (`MeetingStarted`), not the lowercase
  names in the plan; hardened `RabbitSessions` (channel leak on setup failure, unguarded consumer, silent teardown, duplicate
  queue ids).
- T5: no API adjustments; RED run not observed (test and code written together).
- T6: `StoppingProbe` made `@Singleton`; review fixes: start/stop require room membership, start idempotent, stop of an inactive
  meeting a no-op, tests assert the videoserver row survives stop/delete/last-leave.
- T7: no deviations; refused-upgrade tests assert the 401/403 status in the exception.
- T8: no deviations.
- T9: `ConfigSourceContext` visibility of env/sysprops was uncertain, so the factory resolves `ws.consul.enabled`/url and
  `CONSUL_HTTP_TOKEN` sysprop → env (`WS_CONSUL_ENABLED`, `WS_CONSUL_URL`, `CONSUL_HTTP_TOKEN`) → context → default; loading
  stops after the first connection-level failure to avoid sequential per-key timeouts.
- T10: WireMock's and Hamcrest's static `equalTo` clash in `TurnCredentialsTest`, WireMock's qualified; Consul-unreachable WARN
  appears during augmentation (non-fatal).
- T11: `RecordingService` uses Mutiny's default worker pool instead of `ManagedExecutor` (the managed executor propagated the JTA
  transaction into the async task, giving intermittent "Commit invoked while multiple threads active" 500s); post-processing is
  now scheduled only after a successful commit (`JtaAfterCommit`); the `MeetingStopping` observer tolerates videoserver failures;
  403 body assertion dropped (empty body from Quarkus); Hamcrest `equalTo` qualified; SDK 2.4.4-1 resolved on first build.
- T12: Testcontainers 2.x moved classes (`org.testcontainers.postgresql.PostgreSQLContainer`,
  `org.testcontainers.rabbitmq.RabbitMQContainer`); re-augmentation requires the distribution copy to live in a directory
  literally named `quarkus-app` (Quarkus resolves the user-providers directory from it), so `DistributionRunner` copies to
  `<root>/quarkus-app` with logs outside it; `recording.meeting_id` is varchar so the id is bound as String. No jandex or shading
  problems; the shaded jar contains only `com/zextras/carbonio/{videorecorder,ws}`, the migration and `META-INF`.

## Limits of the PoC

Lean core versus CE parity: no media/WebRTC endpoints, no per-user Janus session, no XMPP (message dispatcher is a logging
no-op), no websocket versioning (latest protocol only), no Consul watches (KV read once at startup), no rooms/users/attachments
API (rooms are seeded in tests). The openapi generated by smallrye does not document the enricher fields (`extras`). One RabbitMQ
connection instead of a pool, `exchangeDeclare` on every publish.

Ruled limits (accepted, recorded in the ledger):
- Videoserver robustness (Task 5), all identical to CE's `VideoServerServiceImpl`: a Janus session leaks on partial
  `startMeeting` failure; concurrent starts can race; the DB transaction is held across Janus HTTP calls. To be addressed in the
  full migration.
- Events are published inside the transaction (`MeetingCreated` before flush, recording-stopped websocket event before commit).
- Divergences from CE in Task 6: start/stop require room membership (CE only validates the meeting exists); start is idempotent
  (aligned with the CE CO-4450 fix); stop of an inactive meeting is a no-op (no `MeetingStopping`, no videoserver call, CE stops
  unconditionally); delete stays member-level, not owner (as in CE).
- Divergence in Task 11: the `MeetingStopping` observer logs and swallows Janus/rooms failures (recording still marked STOPPED,
  post-processing still scheduled, possibly with a null serverId), so a broken videoserver cannot block stopping or deleting a
  meeting; in ADV the exception aborted the stop. The explicit `stopRecording` endpoint still propagates errors.
- Auth is opt-in per endpoint (`@Authenticated`); there is no global policy, so a plugin endpoint without the annotation is
  anonymous. `turnCredentials` has no room-membership check (as in ADV) and maps every `TurnServerException` to 404.
- `quarkus/.mvn/maven.config` (`-Dchangelist=-SNAPSHOT`) is a workaround for the root pom's profile-defined `changelist`; it
  disappears if the Quarkus tree moves out of the repo or the root pom changes.
- Plugins are not resolved transitively from `plugins/`: a B1 plugin must be shaded (only the videorecorder SDK here).
- Measurements are single-run, whole MB, `dist.size.mb` taken before re-augmentation, warm cache, Consul unreachable.

Deferred minors worth carrying forward: no partial unique index on `recording` (concurrent start can create two STARTED rows);
`MeetingMapper` N+1 participant queries; `MeetingView.participantUserIds` not defensively copied; duplicate plugin migration ids
and locations not validated; plugin discovery has no caching and uses its own `ObjectMapper`/`HttpClient`; `PluginRegistry` log
text is parsed by the IT; Consul URL not normalized or key-encoded and partial snapshot on interrupt is silent; events may precede
`WebsocketConnected`; UUID parse failure on a user-management payload gives a bare 500; `NotAuthorizedException(message)` puts text
in `WWW-Authenticate`; `DistributionRunner` does not stop processes in a finally block and has TOCTOU on ports; the
context-propagation dependency in `plugin-recording` is now unused and mutiny is undeclared; some test gaps (non-moderator stop,
multi-meeting enrichment, `JtaAfterCommit` direct tests, second websocket connection).

## Recommendation

Use A (build-time assembly) as the default packaging for both CE and ADV, and keep B1 as an opt-in capability rather than the
primary model.

- A is simple and robust: plugins are ordinary Maven dependencies, CDI/Hibernate/Flyway/openapi resolve at build time, no
  version-skew risk at runtime, and `app-adv` is 44 MB against 85 MB for the mutable CE distribution. The cost is a separate
  application artifact per plugin set, which is what CE/ADV packaging already is today.
- B1 works and is cheap at runtime: re-augmentation took 1.8 s, startup 1.4 s with or without the plugin, a plugin jar is
  about 80 KB, and the wrong cases fail loudly (incompatible API) or safely (dropped but not rebuilt is ignored). Its costs are
  elsewhere: the CE package carries `lib/deployment` (about 41 MB, doubling the distribution size), the install flow needs an
  explicit rebuild step, the distribution directory must be named `quarkus-app`, plugins must be shaded, and core and plugins are
  released separately so the API version contract becomes a real compatibility surface.

If B1 is adopted, packaging would need: a PKGBUILD/postinst step that runs `java -Dquarkus.launch.rebuild=true -jar
quarkus-run.jar` on install/upgrade of core or of any plugin (and fails the package script on error); one package per plugin that
installs its shaded jar into `plugins/`; a documented plugin API versioning policy (major bump on breaking `core-api` changes,
`requiredApiVersion` enforced at startup as already implemented); and a CI matrix of core versions times plugin versions, plus a
shading check (no duplicate or transitive dependencies leaking in).

## Roadmap if adopted

1. Full core migration, in this order: remaining REST API (rooms, users, attachments, media/audio/video/screen/hand/iceRestart),
   Janus event listener and per-user sessions, XMPP message dispatcher, websocket versioning, Consul watches, RabbitMQ
   connection pool; fix the videoserver robustness limits listed above while porting (session leak cleanup, start race, no
   transaction across Janus calls) and publish events after commit.
2. Evaluate adoption of `carbonio-quarkus-extensions` (bootstrap, database, rest): it imposes different key conventions than the
   current Consul mapping, so decide whether to adopt its conventions or keep the small `ConfigSource` plus plugin
   `ConsulKeyMapping`; this PoC deliberately left it out.
3. Database history: move the ADV `migration/advanced` series into core plus per-plugin Flyway series for existing installations
   (baseline/repair path from the current `flyway_schema_history`, since the PoC's `recording` V1 is only the final state of
   `V1_0_8`, `V1_2_0` and `V1_3_1`).
4. Packaging: systemd `Type=notify` on Quarkus (`quarkus-systemd-notify`), PKGBUILD, Consul policies/intentions, and a global
   authentication policy instead of per-endpoint opt-in.
5. Contract hardening: openapi documentation for enricher extras, plugin-migration id/location validation, and the deferred
   minors above.
