<!--
SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>

SPDX-License-Identifier: AGPL-3.0-only
-->

# Quarkus plugin PoC — report

Branch `poc/quarkus-plugins` (from `feat/jar-dependency-poc`, merge base `e2a92e1c`). Everything lives in `quarkus/`;
the root Guice build is untouched (`git diff --stat feat/jar-dependency-poc -- . ':(exclude)quarkus' ':(exclude)REUSE.toml'`
prints nothing). Last full run of `mvn -f quarkus/pom.xml clean verify` (after the final-review fixes): BUILD SUCCESS in 1:10 min.

## Update: scope is now the full CE core on Quarkus

The lean slice in `core` was replaced by a mechanical port of the complete CE core (rooms, members, attachments, previews,
meetings with Janus media, websocket events, XMPP token, GraphQL/Consul/Storages clients), keeping CE packages. The plugin
contract (`core-api`), TURN (A), recording (B1), both apps and the B1 IT were kept and re-pointed at the CE classes.

What changed vs the lean slice:
- Guice to CDI (producers for all clients), Jetty/servlet/Boot removed, Ebean to Hibernate/Panache (same CE schema,
  `chats` Flyway, 14 migrations incl. plugin ones), generated CE OpenAPI stubs served by Quarkus REST.
- One auth path: `ZM_AUTH_TOKEN` mechanism + identity provider (one user-management call) feeding the CE `UserPrincipal`;
  CE filters/exception mappers kept; websocket `/events` on quarkus-websockets with handshake auth.
- Wire format: exactly `application/json` plus `X-WSC-API-VERSION`, CE versioning rules.
- Plugin seams implemented over CE classes (`PluginMeetingService`, `PluginMeetingMapper`, gateway, access, publisher).
- Last run `mvn -f quarkus/pom.xml clean verify` (P2): green, 835 tests (core-api 8, core 776, plugin-turn 9, app-ce 3,
  app-adv 9, plugin-recording 26, distribution-it 4). B1 numbers: dist 102 MB, deployment 43 MB, re-augmentation 2.18 s,
  startup 1.91 s CE / 1.85 s with recording. Images: ce 312 MB, adv-a 272 MB, adv-b1 314 MB (compressed).

Parity differences with CE (from P1/P2, to review):
1. Videoserver failures are CE `VideoServerException` (424); lean returned 500.
2. `selectServerId()` returns null (ADV behaviour not ported): no `service-id` header on videorecorder calls.
3. `MeetingStopping` is not fired for inactive meetings (CE hook was a no-op).
4. Websocket queue ids are generated UUIDs per session; container limits applied per session.
5. `Content-Type` charset stripped on every JSON response, plugin endpoints included.
6. Websocket handshake: UM failure answers 500 (status only, not byte-for-byte CE).

Open issues: ADV server selection and videorecorder `service-id` routing; lean `ws.videoserver.*`/`ws.rabbitmq.*` keys still
in `CoreConsulKeys`/test-support; P1 Hibernate follow-ups (EAGER/N+1, merge return values); `MeetingStarted` depends on
`RoomDto.meetingId` being set in `afterMeetingStarted`; building an app module without `-am` yields a broken `quarkus-app`
(build through the reactor). The CE `/health/ready` probes every dependency: ~165 s on macOS with absent dependencies, but
on k3d (all present) the pod was 3/3 Ready 17 s after the restart.

k3d smoke (carbonio-k3d, image `quarkus-ce`, real proxy, user@carbonio.localhost via SOAP AuthRequest cookie):
`GET auth/token`, `users/capabilities`, `rooms?extraFields=members&extraFields=settings`, `meetings` all 200 with
`Content-Type: application/json` exactly and `X-WSC-API-VERSION`. `POST rooms` (one_to_one with admin) 201, `POST meetings` 200,
`GET meetings/{id}` 200, `start` 200, `join` 200 (`ACCEPTED`), `stop` 200. `wss://localhost/services/chats/events` with
subprotocol 1.6.14: 101, subprotocol echoed, `WebsocketConnected` with queueId, ping ok; no cookie 401. A group room needs at
least two other members (CE rule) and the cluster has two users, so the group case answers 400 as in CE. The existing `chats`
schema (history of the lean slice) failed Flyway validation (`1.4.2` not applied) and was dropped (test data only). The ADV
images were built but not deployed in k3d.

## What was built

Maven tree `quarkus/` (Quarkus 3.38.3, Java 21), not part of the root modules. Run everything with `mvn -f quarkus/pom.xml verify`
(Docker required: Testcontainers Postgres/RabbitMQ).

| Module | Content |
|---|---|
| `core-api` | Plugin contract: `PluginApi`/`PluginDescriptor`, `UserPrincipal`, `MeetingView`, `MeetingAccess`, `VideoServerGateway`, `EventPublisher`, `ServiceDiscovery`, `MeetingDtoEnricher`, `PluginMigrations`, `ConsulKeyMapping`, CDI events, async event models generated from `asyncapi.yaml`. Jandex-indexed, no Quarkus implementation. 8 tests. |
| `core` | Ported CE slice: Panache persistence on the CE schema (12 migrations), `ZM_AUTH_TOKEN` auth via the user-management SDK, RabbitMQ events, Janus client + videoserver service, meeting lifecycle REST API, `/events` websocket, plugin registry (API version check, per-plugin Flyway), Consul KV config source (runtime only), OpenAPI via `quarkus-smallrye-openapi`. 70 tests. |
| `test-support` | Shared test helpers (Seed, TestUsers, Stubs, WireMock resource, rabbitmq-mock producer). |
| `plugin-turn` | TURN feature as a plugin (A): client, service, `GET /meetings/{id}/turnCredentials`. Depends on `core-api` only. 9 tests. |
| `plugin-recording` | Recording feature as a plugin (B1): entity/repository, videorecorder client, Janus DTOs, start/stop endpoints, `MeetingStopping` observer, enricher, V1 migration. Shaded with the videorecorder SDK only. 26 tests. |
| `plugin-incompatible` (+ `test-plugins`) | Plugin declaring `requiredApiVersion` 2.0, used for the fail-fast test. |
| `app-ce` | Application = `core`, `mutable-jar`, `quarkus.package.jar.user-providers-directory=plugins`. 3 tests (plugin absent, OpenAPI). |
| `app-adv` | Application = `core` + `plugin-turn`, standard fast-jar. 9 tests. |
| `distribution-it` | Failsafe IT driving the B1 cycle with external processes on the real `app-ce` distribution. 4 tests. |

Total: 129 automated tests across the tree (core-api 8, core 70, plugin-turn 9, plugin-recording 26, app-ce 3, app-adv 9, distribution-it 4, from the surefire/failsafe reports), all green.

## Plugin contract

| Guice seam (CE/ADV today) | `core-api` replacement as implemented | Fit |
|---|---|---|
| `stopActiveRecording` hook | Synchronous CDI event `MeetingStopping(meetingId, userId)`, fired before `videoServer.stopMeeting` | Fits. Fired from stop, delete and last-leave, only when the meeting is active. |
| `afterMeetingStarted` | CDI event `MeetingStarted` | Fits (no plugin needs it yet). |
| `MeetingMapperImpl.enrich` | `MeetingDtoEnricher` collected via `Instance<>`; `MeetingDto.extras` with `@JsonAnyGetter` | Fits: `recStartedAt`/`recUserId` stay top-level. Not documented in OpenAPI (observed, see A). |
| `VideoServerServiceImpl` plugin-message helpers, `getMeetingServerId` | `VideoServerGateway` (`roomsOf(meetingId) -> Optional<VideoServerRooms>`, `sendVideoRoomMessage`, `sendAudioBridgeMessage`) | Fits: plugin owns its Janus DTOs. The gateway is not transactional around the Janus call (see Problems, final review). |
| Recording validation in the advanced service | `MeetingAccess.requireActiveMeetingModerator` → `MeetingView` | Fits. |
| `eventDispatcher.sendToUserExchange` | `EventPublisher.sendToUsers` | Fits. |
| `discoveryService.isServiceAvailable` | `ServiceDiscovery.isAvailable` | Fits. |
| `Multibinder<ConfigContribution>` | MicroProfile Config + `ConsulKeyMapping` ServiceLoader file contributed by the plugin | Fits, but needs the extra ServiceLoader file (see Problems). |
| Separate `migration/advanced` Flyway series | `PluginMigrations(name, location)`; one Flyway per plugin, history table `flyway_history_<plugin>`, run in a `StartupEvent` observer after core migrations | Fits: FKs to `CHATS.MEETING` valid. Duplicate ids/locations are not validated. |
| — | `PluginDescriptor` + `requiredApiVersion`; fail fast on incompatibility | New. Essential for B1. |

What did not fit cleanly: the `ConfigSourceFactory` runs at augmentation too: it needed explicit sysprop/env handling for Consul
settings and, worse, Quarkus recorded the Consul values it read at build time into the generated bytecode (secrets baked into
the jar, see B1), so it is now a no-op outside the application runtime; `StoppingProbe`-style test beans must be `@Singleton`, not `@ApplicationScoped` (client proxies hide public fields); the
403 message of `ForbiddenException(message)` is not rendered in the body by Quarkus.

## A — build-time assembly (TURN)

What worked: `plugin-turn` depends only on `core-api`; `app-adv` adds it as a Maven dependency and the standard fast-jar build
discovers its beans, resource and config through Jandex. No runtime cost beyond the plugin's own classes.

Evidence: `app-adv` `TurnCredentialsTest` (7) covers 200 with a mock turn server, 204 when `carbonio-turn-server-auth` is not
available, 404 on missing serverId / `TurnServerException`, fallback to `getCredentialsAny` on 503/404/0; `TurnUnavailableTest`
(1); `OpenApiTest` (1); `plugin-turn` unit tests (9). In `app-ce` the same endpoint is absent (`PluginAbsentTest`, 3).

OpenAPI (`quarkus-smallrye-openapi`, added in the final review; `GET /q/openapi?format=json`, observed in the module tests):
plugin JAX-RS resources are documented like core ones. `app-adv` lists `/meetings/{meetingId}/turnCredentials`, `app-ce` lists
only core paths (`/meetings...`, `/plugins`), the `plugin-recording` test application lists `startRecording`/`stopRecording` and
the `RecordingFields` schema, and in B1 the re-augmented `app-ce` gains `startRecording` (asserted in the IT; the document is
produced at augmentation). Enricher extras are **not** documented: in every application the `MeetingDto` schema has only its
declared properties (`active, createdAt, id, meetingType, name, participants, roomId, startedAt`), no `recStartedAt`/`recUserId`
and no `additionalProperties`, because `extras` is a `@JsonAnyGetter` map.

Cost: `app-adv` fast-jar is 45 MB (`du -sh quarkus/app-adv/target/quarkus-app`), versus 90 MB for the `app-ce` mutable-jar
(which also carries `lib/deployment`, 45 MB); both grew by 1–5 MB when smallrye-openapi was added in the final review. Adding or removing a plugin means rebuilding the application artifact.

## B1 — re-augmentation (recording)

Operator flow: copy `plugin-recording-*-plugin.jar` into `quarkus-app/plugins/`, run
`java -Dquarkus.launch.rebuild=true -jar quarkus-run.jar`, restart the application normally.

Measurements (re-run in the final-review fix wave with smallrye-openapi included, `distribution-it/target/b1-metrics.properties`,
now written only when every IT step passed; single run, whole MB, `dist.size.mb` measured before re-augmentation, warm cache,
no Consul):

| Metric | Value |
|---|---|
| `reaugment.seconds` | 1.87 |
| `startup.ce.seconds` | 1.71 |
| `startup.recording.seconds` | 1.392 |
| `dist.size.mb` | 89 (du: 90 MB) |
| `deployment.size.mb` | 44 (du: 45 MB `lib/deployment`) |
| plugin jar (shaded) | 81,486 bytes (about 80 KB) |

Evidence (`RecordingPluginB1IT`, 4 tests, all green):
1. CE distribution: `/plugins` is empty, `startRecording` returns 404 and is absent from `/q/openapi`, no `chats.recording` table.
2. Jar dropped but not rebuilt: ignored. `/plugins` still empty, no table.
3. After re-augmentation and restart: log line `Loaded plugin recording 0.1.0 (API 1.0)`, `flyway_history_recording` and
   `chats.recording` created, `startRecording` 204 with the Janus mock receiving `edit` (h-video), `enable_mjrs` (h-audio) and
   `enable_recording` (h-video), `/q/openapi` lists `startRecording`, `GET /meetings/{id}`
   exposes `recStartedAt`, stopping the meeting leaves the recording STOPPED and calls the videorecorder mock with the
   `service-id` header.
4. Incompatible plugin (`requiredApiVersion` 2.0): the process exits non-zero with
   `Plugin 'incompatible' 0.1.0 requires plugin API 2.0 but core provides 1.0`.

Consul and re-augmentation (final review, verified empirically): the Consul `ConfigSourceFactory` is discovered by SmallRye at
build time too, and `%prod.ws.consul.enabled=true` is active while packaging. Re-augmenting a copy of `app-ce`
(`WS_CONSUL_URL=<stub> java -Dquarkus.launch.rebuild=true -jar quarkus-run.jar`) against a stub answering every
`/v1/kv/<key>?raw=true` with `SECRETMARKER_<key>` made the build issue 7 KV reads, and all 7 marker values (DB username/password,
RabbitMQ username/password, Janus api-secret, bitrate, bitrate-cap) ended up as runtime defaults in
`quarkus/generated-bytecode.jar` (`io/quarkus/runtime/generated/RunTimeConfigCustomizer.class`). So a B1 postinst rebuild with
Consul reachable would have baked the credentials into the jar on disk (and frozen them as defaults). Fix: the factory returns no
source unless `ExecutionMode.current()` is `RUNTIME_INIT` or `RUNNING`; the generated `ApplicationImpl.doStart` calls
`ExecutionModeManager.runtimeInit()` right before building the runtime config, while augmentation never runs the application
(mode stays `UNSET`). After the fix the same re-augmentation makes 0 KV requests and the jar contains no marker; starting the
re-augmented distribution still reads all 7 keys from Consul at runtime.

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
- Final review fixes: (1) `VideoServerGatewayImpl` send methods were `@Transactional(REQUIRED)`, so a Janus failure marked core's
  transaction rollback-only and the recording plugin's lenient catch could not save a meeting stop/delete (observed: 500,
  `Syncs are not allowed to be registered when the transaction is in state 1`); only the row lookup now joins the transaction
  (`QuarkusTransaction.joiningExisting()`), the Janus call runs outside, and `core-api` javadoc (`MeetingAccess`,
  `VideoServerGateway`) documents the rollback-only pitfall; (2) Consul secrets baked at augmentation, see B1; (3)
  `quarkus-smallrye-openapi` was missing although the report discussed OpenAPI, now added and observed (see A); deleting an
  inactive meeting no longer fires `MeetingStopping` nor calls the videoserver; `plugin-recording` declares `mutiny` (provided,
  BOM-managed) and no longer the unused context-propagation extension; the B1 IT runner stops processes with a
  `destroyForcibly` fallback and does not write metrics after a failure.
- T12: Testcontainers 2.x moved classes (`org.testcontainers.postgresql.PostgreSQLContainer`,
  `org.testcontainers.rabbitmq.RabbitMQContainer`); re-augmentation requires the distribution copy to live in a directory
  literally named `quarkus-app` (Quarkus resolves the user-providers directory from it), so `DistributionRunner` copies to
  `<root>/quarkus-app` with logs outside it; `recording.meeting_id` is varchar so the id is bound as String. No jandex or shading
  problems; the shaded jar contains only `com/zextras/carbonio/{videorecorder,ws}`, the migration and `META-INF`.

## Limits of the PoC

Lean core versus CE parity: no media/WebRTC endpoints, no per-user Janus session, no XMPP (message dispatcher is a logging
no-op), no websocket versioning (latest protocol only), no Consul watches (KV read once at startup), no rooms/users/attachments
API (rooms are seeded in tests). The OpenAPI document generated by smallrye-openapi does not document the enricher fields
(`extras`, observed: `MeetingDto` lists only its declared properties). One RabbitMQ
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
  post-processing still scheduled, possibly with a null serverId), so the recording plugin's own Janus calls cannot block
  stopping, leaving or deleting a meeting (tested with Janus answering HTTP 500 to `enable_mjrs` on stop and delete); in ADV the
  exception aborted the stop. The explicit `stopRecording` endpoint still propagates errors (tested: 500, recording stays
  STARTED). This is not a general guarantee: core's own `VideoServerService.stopMeeting` still propagates Janus failures (room
  destroy/detach), so a broken videoserver still fails stop/leave/delete with 500, exactly as in CE.
- Auth is opt-in per endpoint (`@Authenticated`); there is no global policy, so a plugin endpoint without the annotation is
  anonymous. `GET /plugins` is deliberately `@PermitAll`: it exposes the installed plugin ids and versions to anonymous callers
  (kept for the PoC; restrict or drop versions in a real deployment). `/q/openapi` is also anonymous by Quarkus default.
  `turnCredentials` has no room-membership check (as in ADV) and maps every `TurnServerException` to 404.
- `quarkus/.mvn/maven.config` (`-Dchangelist=-SNAPSHOT`) is a workaround for the root pom's profile-defined `changelist`; it
  disappears if the Quarkus tree moves out of the repo or the root pom changes.
- Plugins are not resolved transitively from `plugins/`: a B1 plugin must be shaded (only the videorecorder SDK here).
- Measurements are single-run, whole MB, `dist.size.mb` taken before re-augmentation, warm cache, Consul unreachable.
- Consul is read only at application runtime (never at augmentation); detection relies on Quarkus' internal
  `ExecutionMode`, an internal lifecycle signal that should be re-checked on Quarkus upgrades (the factory unit tests pin the
  augmentation-like `UNSET` case).

Deferred minors worth carrying forward: no partial unique index on `recording` (concurrent start can create two STARTED rows);
`MeetingMapper` N+1 participant queries; `MeetingView.participantUserIds` not defensively copied; duplicate plugin migration ids
and locations not validated; plugin discovery has no caching and uses its own `ObjectMapper`/`HttpClient`; `PluginRegistry` log
text is parsed by the IT; Consul URL not normalized or key-encoded and partial snapshot on interrupt is silent; events may precede
`WebsocketConnected`; UUID parse failure on a user-management payload gives a bare 500; `NotAuthorizedException(message)` puts text
in `WWW-Authenticate`; `DistributionRunner` has TOCTOU on ports and its readiness HTTP client has no timeouts; some test gaps (non-moderator stop,
multi-meeting enrichment, `JtaAfterCommit` direct tests, second websocket connection).

## Recommendation

Use A (build-time assembly) as the default packaging for both CE and ADV, and keep B1 as an opt-in capability rather than the
primary model.

- A is simple and robust: plugins are ordinary Maven dependencies, CDI/Hibernate/Flyway/openapi resolve at build time, no
  version-skew risk at runtime, and `app-adv` is 45 MB against 90 MB for `app-ce`. That difference is the ~45 MB `lib/deployment` that
  `mutable-jar` keeps; a non-mutable CE would be about the size of `app-adv`. The cost is a separate application artifact per
  plugin set.
- B1 works and is cheap at runtime: re-augmentation took 1.9 s, startup 1.71 s without and 1.39 s with the plugin (no measurable difference, single-run noise), a plugin jar is
  about 80 KB, and the wrong cases fail loudly (incompatible API) or safely (dropped but not rebuilt is ignored). Its costs are
  elsewhere: the CE package carries `lib/deployment` (about 44 MB, doubling the distribution size), the install flow needs an
  explicit rebuild step, the distribution directory must be named `quarkus-app`, plugins must be shaded, and core and plugins are
  released separately so the API version contract becomes a real compatibility surface.

If B1 is adopted, packaging would need: a PKGBUILD/postinst step that runs `java -Dquarkus.launch.rebuild=true -jar
quarkus-run.jar` on install/upgrade of core or of any plugin (and fails the package script on error), with any build-time
config source kept away from secrets (the Consul source is already runtime-only, see B1), since the rebuilt jar stays on disk; one package per plugin that
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
