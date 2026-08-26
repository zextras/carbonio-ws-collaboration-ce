// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

library(
    identifier: 'jenkins-lib-common@v4.9.2',
    retriever: modernSCM([
        $class: 'GitSCMSource',
        credentialsId: 'jenkins-integration-with-github-account',
        remote: 'git@github.com:zextras/jenkins-lib-common.git',
    ])
)

// carbonio-ws-collaboration-ce uses a maven-shade fat JAR
// (carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-ce-*-fatjar.jar),
// not a Quarkus *-runner.jar. dt3_pipeline's jarBuild copies only *-runner.jar patterns, so we use
// appModule: 'carbonio-ws-collaboration-boot' to enable the Java build stage and handle the JAR copy
// via packaging.preBuildScript (runs in the yap container after workspace unstash, before
// yap build). buildFlags: '-ds' skips makedep/dep resolution — no Zextras makedeps in PKGBUILD
// (only runtime depends), so Zextras repo injection is not needed.
dt3_pipeline(
    repoName: 'carbonio-ws-collaboration-ce',
    appModule: 'carbonio-ws-collaboration-boot',
    packaging: [
        buildFlags: '-ds',
        preBuildScript: '''
            cp -a carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-ce-*-fatjar.jar package/carbonio-ws-collaboration-ce.jar
        ''',
    ],
    docker: [[
        dockerfile: 'docker/wsc/Dockerfile',
        imageName: 'carbonio-ws-collaboration-ce',
        title: 'Carbonio WS Collaboration CE',
        description: 'Carbonio WS Collaboration CE',
        platforms: ['linux/amd64', 'linux/arm64'] as Set,
    ]],
    reuse: [projectType: 'CE'],
    flywayGuard: [
        migrationPaths: ['carbonio-ws-collaboration-core/src/main/resources/migration'],
    ],
)
