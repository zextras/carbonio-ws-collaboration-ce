## [1.11.2](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.11.1...v1.11.2) (2026-06-23)

### Bug Fixes

* simplify session management by removing user session indexing ([#263](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/263)) ([210e17c](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/210e17c02e45d44e7325b1d4c30dbe39f6430bfe))

## [1.11.0](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.10.0...v1.11.0) (2026-06-19)

### Features

* **ci:** [IN-951] add arm64 platform to docker image builds ([#236](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/236)) ([d769a33](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/d769a33bf9bad513c44d67e56059c82afb06c45a))

### Bug Fixes

* **deps:** update dependency ch.qos.logback:logback-classic to v1.5.34 ([#195](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/195)) ([ae22446](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/ae22446a7053290b2284a782871807ffe71eb5ee))
* **deps:** update dependency org.hibernate.validator:hibernate-validator-cdi to v8.0.3.final ([#202](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/202)) ([dca0930](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/dca093036c382b444bfe8a425d80b060eec7b4e2))
* **reuse:** project-owned REUSE.toml (drop catch-all + legacy dep5) ([#257](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/257)) ([455a29a](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/455a29a7f16789bdd2600862da1272e4771b9502))

## [1.10.0](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.9.1...v1.10.0) (2026-05-28)

### Features

* add iceRestartScreen endpoint for WebRTC ICE restart in meetings ([#235](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/235)) ([9ce8771](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/9ce87717d4007ce2c71e840c9aa4a8201e61e36b))

## [1.9.1](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.9.0...v1.9.1) (2026-05-27)

### Bug Fixes

* **deps:** add explicit service-discover-base dependency ([#233](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/233)) ([f635bd8](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/f635bd849450acae53102794c886711e06a03d93))

## [1.9.0](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.8.2...v1.9.0) (2026-05-19)

### Features

* add attachment gallery API with paginated list and bulk delete for room attachments ([#231](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/231)) ([be2d0b9](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/be2d0b9cbe498f917d3bbbb6b5d058a80ba390f2))
* **meetings:** add user feedback when RabbitMQ disconnects ([#230](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/230)) ([946ab94](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/946ab94f2c887f345e60266fe54f841559ec7ca0))

### Bug Fixes

* prevent XXE attacks in XML message parsers ([#225](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/225)) ([5e9d1ac](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/5e9d1acf83ac55505c4a22a6f008cf04dfe00a87))

## [1.8.2](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.8.1...v1.8.2) (2026-05-06)

### Bug Fixes

* restore buildPackages() to fix pkgrel on tag builds ([#226](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/226)) ([25d545e](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/25d545e084cbdf3a67d82e0406c664c4e0804640))

## [1.8.1](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.8.0...v1.8.1) (2026-05-05)

## [1.8.0](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.7.1...v1.8.0) (2026-05-05)

### Features

* add decline API, add meetingStart, meeting ended and meeting declined xmpp messages ([#222](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/222)) ([092de39](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/092de39cad1152ba4321907fbd59dd99a61042a6))
* adopt carbonio-systemd-notify for native sd_notify readiness ([#215](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/215)) ([7418633](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/7418633d025a001bee6105549a9c69502170b6a8))
* expose user capabilities API ([#191](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/191)) ([#211](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/211)) ([2a2c2d0](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/2a2c2d0a1c530e5a649b31b678afc1d76fbe3549))
* migrate to gRPC UM SDK ([#212](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/212)) ([2457eda](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/2457edacfd8c7aa72ff31ad4a19f091f09b172c6))
* systemd hardening and service-discover.target orchestration ([#214](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/214)) ([adcad7f](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/adcad7f379168b2d00ed509530e5589ad3a828e3))

### Bug Fixes

* add ServicesResourceTransformer to shade plugin for gRPC ([#213](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/213)) ([f39bf7a](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/f39bf7a720df35f94e98fc74a940befca0cdb17c))
* remove non-owners internal users from temporary room when they leave the meeting ([#196](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/196)) ([#216](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/216)) ([c8b8083](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/c8b8083c301c830e52fa5d1426006300eb750539))

## [1.7.1](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.7.0...v1.7.1) (2026-03-02)

### Bug Fixes

* jar build with profile in Jenkinsfile ([#209](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/209)) ([87e0526](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/87e0526aeeee99c37446989114730029f06623f7))

## [1.7.0](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.6.3...v1.7.0) (2026-02-23)

### Features

* clear full history on temporary room ([#206](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/206)) ([595ae21](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/595ae21151e2957bd64bf76220655bd5752d587d))
* implement ICE restart functionality ([#204](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/204)) ([72a1d95](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/72a1d959698ddf2dff1b71b57b9da821574ddc19))

### Bug Fixes

* repo url in releaserc.json ([#207](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/207)) ([a2893bc](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/a2893bc79d21304a51a1ad320bcde1d7f2ca5f13))
* use ChatsIdentifier on file copy ([#197](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/197)) ([eb5511b](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/eb5511bfe405ca7b3bea3ef02a8c6136f17d7832))

## [](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/v1.6.3...v) (2026-01-14)
## [1.6.3](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/0.6.0...v1.6.3) (2025-11-12)

### ⚠ BREAKING CHANGES

* remove user avatar support (#140)
* update insert room and add member APIs (#123)

### Features

* add cache to store videoserver session ([#114](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/114)) ([37ef91b](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/37ef91b93a2f24fc3d0df7d76a807f0906e39bd7))
* add ubuntu 24.04 (ubuntu-noble) support ([#115](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/115)) ([6884187](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/68841871a921140315f16fea14f6bb4db21d6df8))
* create docker compose and Dockerfile for wsc ([#145](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/145)) ([582081a](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/582081ab4bb20c58847acd25084863187ebe419a))
* expose bitrate and bitrate-cap configs for video server on k/v consul ([#172](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/172)) ([044916b](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/044916b71f10f13b2c58a3476cfe6601e198af75))
* implement api versioning ([#143](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/143)) ([9809555](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/98095556f0aa458eb0d4b3950c588b82bc01a490))
* implement new API to insert attachment in multipart/form-data ([#148](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/148)) ([f8de943](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/f8de943d1d557fe94f1dece6751b3bc13120dcf2))
* implement new bulk API to update room owners ([#131](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/131)) ([0d64d2e](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/0d64d2e74dd1e17a4343f4a7ad436b2614a9f93f))
* implement new raise hand API for meetings ([#88](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/88)) ([#132](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/132)) ([67da80e](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/67da80eb44d560e319e9be79519dc1f460044560))
* implement websocket api versioning ([#151](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/151)) ([f136a67](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/f136a67ba92af4851d925c7e8cc5c314c79e8c62))
* improve raise hand API storing hand order ([#94](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/94)) ([#137](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/137)) ([90d0f5e](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/90d0f5ed588dc410d70888f9f919f3190e2436b3))
* remove user avatar support ([#140](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/140)) ([72756d1](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/72756d1a7d1a7bef3f4dd9d9c63ad81a2ac713fa))

### Bug Fixes

* [WSC-1838] Memory leak websocket rabbitmq ([#129](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/129)) ([e61f4d8](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/e61f4d8d9db3927cf25e308fc7af2d638a73b3b2))
* close streams after files are retrieved by clients using StreamingOutput ([#120](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/120)) ([e704d19](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/e704d19a2886e2a9fb7f82a627f25a731c093f79))
* filter out non active/non feature-enabled users ([#173](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/173)) ([56f8988](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/56f89886370537ba07cee6285e72f60ef6f803a2))
* JAVA_HOME path in carbonio-ws-collaboration script ([#113](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/113)) ([8c9fbd0](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/8c9fbd0c6b0ef3b1c3c9b52e506d7972007f6f39))
* leak memory cancelling scheduled ping task on session close ([#125](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/125)) ([#163](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/163)) ([e2a9ec5](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/e2a9ec5fe0411150488108a309d3ad3579c72952))
* move jar from /usr/bin to /usr/share to follow the FHS standard ([#112](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/112)) ([de29a32](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/de29a32c023dbb51f8f4f72a211474458e01c191))
* pass file owner id on attachment preview ([#168](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/168)) ([db630bd](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/db630bddac69156e12fa2a38db5cc3ac0e2bd3e9))
* remove all room members to destroy the room on message-dispatcher ([#93](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/93)) ([#136](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/136)) ([f862f74](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/f862f744df9f29da715c6d923e6f2a3f8107c0e2))
* remove non moderator users from temporary rooms when meeting ends ([#171](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/171)) ([c98e64f](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/c98e64f2fbd487fe57d3b4ca9df69d9bc1bd51a0))
* resizing window/enable screen share during a call will broke the webcam view of other partecipant ([236f86d](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/236f86d98d811a3e76c8d378fc3204121388a17c))
* revert WantedBy for compatibility with older systems ([#152](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/152)) ([99c0c13](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/99c0c13f0eb6e56832864b988e51f7ba9d5e7a81))
* save file metadata on db with decoded filename ([#150](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/150)) ([ffd3980](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/ffd39805b9b42ab649181a772a94364dd02b607a))
* send subscribed events only for VIDEO_IN media track type ([#167](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/167)) ([daef809](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/daef8090e6dc8491f44151c3c3c9d26e1d5c4f32))
* set UUID type for list of users in getUsers request ([#156](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/156)) ([51ab2f2](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/51ab2f29bd552b2bda81a8039682dedd5619e043))
* skip subscribed events with streams originated by video_out or screen sessions ([#160](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/160)) ([bce5d67](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/bce5d67456958aed942ca4509529a7642e4d7d69))

### Performance Improvements

* add maxLifetime hikari in app config ([#139](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/139)) ([2b2bf3d](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/2b2bf3d190ac91bba90c5947211b7e8a0db2d747))

### Miscellaneous Chores

* update insert room and add member APIs ([#123](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/123)) ([c91c516](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/c91c51618954d369eb289d00fcb2ecc82f80bc34))
## [0.6.0](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/0.5.1...0.6.0) (2024-06-17)

### Features

* Add missing indexes ([#104](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/104)) ([18bbed3](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/18bbed38d16b858605b94522b971dfe9de506468))
## [0.5.1](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/0.5.0...0.5.1) (2024-04-30)

### Features

* Block the creation of a meeting if another one is already present for that room ([#99](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/99)) ([3facd06](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/3facd06cc94641ecae569881c8625d2644d951dd))

### Bug Fixes

* Use always an owner to remove someone from rooms on mongoose ([#100](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/100)) ([a86379d](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/a86379dbdb2d0eddc58c09c0c1ceb801621b407a))
## [0.5.0](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/0.3.6...0.5.0) (2024-04-12)

### Features

* add memory parameters ([#98](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/98)) ([a0b5796](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/a0b5796795ef22ecba132e9466cf43a82d261181))
* Upgrade all problematic dependencies with switch to Jakarta namespace ([#92](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/92)) ([6c1e593](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/6c1e5934bb629e47bdf445085e64fe9ba3e02dc3))
* WSC-1281 add start timestamp to Meeting ([#96](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/96)) ([49cc59b](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/49cc59b7d7244d203e38b3f817b8b5c49a71a776))

### Bug Fixes

* Add response result on join ([#97](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/97)) ([35e6dda](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/35e6dda901d8a91e9c589b0e8385b173a039ff1c))
* downgrade openapi to 6.4.0 ([#95](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/95)) ([428d76d](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/428d76dc9452adf65525e61d96374fdcc7af307c))
## [0.3.6](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/0.3.5...0.3.6) (2024-03-19)

### Features

* Refactor creation flow of meetings ([#84](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/84)) ([7c54a2d](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/7c54a2d655c5f8523b440c546467392b4eb6762a))

### Bug Fixes

* *.hcl: apply corrections to validate with hclfmt ([#86](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/86)) ([7d7f4ee](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/7d7f4ee345696643fe9ebf50efe65e0ce1f56749))
* ci: typo on promotion target repo ([d958d6d](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/d958d6da76784ecccfaf5ad75da67a9d4a5cbd0c))
* increase mime_type column length to 256 ([#85](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/85)) ([1074787](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/1074787558c4140c3260e3119a66179d8d098ae0))
## [0.3.5](https://github.com/zextras/carbonio-ws-collaboration-ce/compare/c70e35a28682cb5a30991a5956ee29e68a0827d9...0.3.5) (2024-01-04)

### Features

* [CHATS-384] [CHATS-385] Implement data model and REST for Janus plugins ([e7e8bee](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/e7e8beecd0c3fa43c1d10e44176950d15f99c702))
* [CHATS-465] implement join meeting API ([83d99b9](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/83d99b95b7a039b642296344cf3cc22956fb8055))
* [CHATS-466]  Add an endpoint to create a meeting for specified room ([3354cbf](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/3354cbf97e13c632e68ddcf09435dda95d104b7a))
* [CHATS-466] Add intergrations tests for meeting creation endpoint. ([f15ece8](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/f15ece88ffbaae92aad3a6c1f473e1e525a9ad97))
* [CHATS-467] implement leave meeting ([d1beef2](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/d1beef2c9b1f06967f76a84adee00a9d440e7bde))
* [CHATS-468] implement the businnes logic to delete meeting API ([4633a2c](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/4633a2cf25947007c8a9ded8ed69bb08dfb45968))
* [CHATS-477] Add type filed to connected event. ([6c267b5](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/6c267b502785881921c2fb6b135d80ff65ef7ce7))
* [CHATS-483] add endpoint to retrieve all meeting that authenticated user can join ([18c1259](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/18c1259fe74e5ff0cf8f8a06f0d65487bf6950e4))
* [CHATS-484] allow open and close the audio stream ([58248ad](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/58248adb2fb344ed6a18909698da7837e360b004))
* [CHATS-484] allow open and close video stream ([5ca1099](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/5ca1099eec50015dd642e016f01316c1a8fdeb54))
* [CHATS-486] allow a session to open or to close the screen share stream ([2cd4d09](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/2cd4d095e96d1abfb424b33bb62fe97e0b2c9582))
* [CHATS-617] remove the codebase related to workspace ([c1c2330](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/c1c2330fad200ed29ffc1a11ae8208a2457ff700))
* [CHATS-817] remove the hash from user and room ([85d492a](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/85d492add10354a2b595faacacf7288504a1815e))
* [CHATS-821] fix error on forwarding when there are new lines or special characters in the body ([c162bbf](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/c162bbf2d3e0aa193a8852e8c4516ae41cc0c69d))
* [CHATS-835] add rabbitmq connection configuration on consul ([#36](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/36)) ([23677c0](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/23677c0f065f4a6053997e4975725315743c0fa9))
* [WSC-1007] accept GIF as preview output format ([06e812e](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/06e812e135bfc3326615cd8b780123bc105a8626))
* [WSC-1021] add XMPP message for affiliate changes ([#58](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/58)) ([acf775f](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/acf775f19cadb5669160d0bedb2b272a59d20322))
* [WSC-502] refactor meeting apis and add janus apis ([#48](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/48)) ([8a03780](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/8a03780b610142da8bbeaba7952d9c39dec299f9))
* [WSC-855] forward a message multiple times ([#41](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/41)) ([24efe06](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/24efe06f8d023965113f6433498c772c2bf6b613))
* [WSC-988] encode XMPP messages with UTF-8 ([a928f99](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/a928f9972c31460faa4a729722d595f923c0297d))
* add meeting disconnection in EventsWebSocketEndpoint ([#68](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/68)) ([b707266](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/b7072668369080b3f1ec69d60899dfcc853e0195))
* Add sonarqube analysis ([5176520](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/51765201ab5fd86a00caa89d8d568995d8412d51))
* Api refactor definition for creating and joining a meeting ([#45](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/45)) ([dd753d1](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/dd753d1acc7335eee0fa73c1dc8e37d602787175))
* CHATS-735 Rename from chats to ws-collaboration ([a9998e7](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/a9998e7a8fc9c7a36ccdf10273d435f68b8a55af))
* Create a new type of room  ([#81](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/81)) ([13f65d7](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/13f65d745ef360c46949a92fdc56e21d50144fab))
* move to yap agent and add rhel9 support ([#74](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/74)) ([ea90acb](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/ea90acb5a1a6a095fa384ffd43f2420e66e3b2be))
* unify video and screen apis ([#56](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/56)) ([0a424c9](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/0a424c91d51476a2513c36daa504a01004ac8499))
* WSC-1003 update pre-commit-config yaml ([#64](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/64)) ([94249c6](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/94249c6f9f546473132d202ec4f78ed612e56ca6))
* WSC-1004/1026 Refactor WSC events and add missing ones for meetings ([#60](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/60)) ([3853ed1](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/3853ed156bcea3040867a8fb5e3714ffef423e87))
* WSC-1146 Add video-call configs ([#76](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/76)) ([4dc22bb](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/4dc22bb1fe019c9fc12a48f8efbbf2abb3637dae))
* WSC-1155 let users re-enter in meetings that they've already joined ([#77](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/77)) ([769b919](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/769b91936c9b6eff2adcf21106b8f2ae68fe2e48))
* WSC-944 remove session id from Apis and events ([#65](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/65)) ([c9c3117](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/c9c3117dbd639425f18950591dec6c3a405986be))
* WSC-978 Add VideoServerEventListener ([#61](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/61)) ([57fda7e](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/57fda7e50c259ceb5b0e7a7a1f744aefb373f92d))

### Bug Fixes

* [CHATS-719] Add delay between requests to populate consul k/v client cache ([3b40180](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/3b4018051a42b7bd774f31345581221641333fc9))
* [CHATS-785] forward bug ([164c169](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/164c16956b0f84139039fad396056426e0ac15ac))
* bug when user joins more than one meeting enabling video ([#80](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/80)) ([5689903](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/5689903b6dcc9c43c79d6bfb054bdb41e11c15fe))
* Change return type of start an stop endpoints ([#55](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/55)) ([14c8c11](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/14c8c11e547095001172d35b5e3c84aeb6bcb4e4))
* consul test ([a139e5b](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/a139e5b4825ce2416af8a51804fb6362f3ff9499))
* janus events in VideoServerEventListener ([#66](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/66)) ([fd637f8](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/fd637f83fb13edff7be688eb645cd0d002faabfb))
* replace old deprecated publishCoverage with recordCoverage in JenkinsFile ([#82](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/82)) ([9b1fa6f](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/9b1fa6f0e24373ddc858985053ece95cd53a1125))
* video stream publish and subscription race condition ([#79](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/79)) ([aaca2c9](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/aaca2c90f3389cc2d68b6d075043bfe83e80ddbf))
* WSC-1093 leave and delete room ([#70](https://github.com/zextras/carbonio-ws-collaboration-ce/issues/70)) ([a665b31](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/a665b317e39cfdb6651a068718d2e6cdc073aadf))

### Reverts

* Revert "[CHATS-143] Replaced resteasy client with HttpClient" ([c70e35a](https://github.com/zextras/carbonio-ws-collaboration-ce/commit/c70e35a28682cb5a30991a5956ee29e68a0827d9))
