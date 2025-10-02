// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

library(
  identifier: 'jenkins-packages-build-library@1.0.4',
  retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'git@github.com:zextras/jenkins-packages-build-library.git',
    credentialsId: 'jenkins-integration-with-github-account'
  ])
)

pipeline {
  agent {
    node {
      label 'zextras-v1'
    }
  }

  environment {
    FAILURE_EMAIL_RECIPIENTS = 'smokybeans@zextras.com'
    NETWORK_OPTS = '--network ci_agent'
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '5'))
    skipDefaultCheckout()
    timeout(time: 1, unit: 'HOURS')
  }

  parameters {
    booleanParam defaultValue: false,
      description: 'Whether to upload the packages in playground repository',
      name: 'PLAYGROUND'
    booleanParam defaultValue: false,
      description: 'Whether to run the dependency check',
      name: 'DEPENDENCY_CHECK'
  }

  tools {
    jfrog 'jfrog-cli'
  }

  stages {
    stage('Build setup') {
      steps {
        container('jdk-17') {
          checkout scm
          script {
            gitMetadata()
          }
        }
      }
    }

    stage('Compiling') {
      steps {
        container('jdk-17') {
          sh '''
            mvn -Dmaven.repo.local=$(pwd)/m2 -N wrapper:wrapper
            mvn -Dmaven.repo.local=$(pwd)/m2 -T1C -B compile
            mvn package -Dmaven.main.skip -Dmaven.repo.local=$(pwd)/m2
            cp carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-ce-fatjar.jar package/
          '''
        }
      }
    }

    stage('Testing') {
      steps {
        container('jdk-17') {
          sh '''
            mvn -B \
            -Dlogback.configurationFile="$(pwd)"/carbonio-ws-collaboration-boot/src/main/resources/logback-test-silent.xml \
            verify
          '''
          recordCoverage(tools: [[pattern: 'target/site/jacoco-all-tests/jacoco.xml']])
        }
      }
    }

    stage('Sonarqube Analysis') {
      steps {
        container('jdk-17') {
          withSonarQubeEnv(credentialsId: 'sonarqube-user-token', installationName: 'SonarQube instance') {
            sh '''
              mvn -Dsonar.coverage.jacoco.xmlReportPaths=../target/site/jacoco-all-tests/jacoco.xml \
                -B sonar:sonar
            '''
          }
        }
      }
    }

    stage('Build and Publish Docker Image') {
      when {
        anyOf {
          branch 'devel'
          buildingTag()
          expression { params.PLAYGROUND == true }
        }
      }

      steps {
        container('dind') {
          withDockerRegistry(credentialsId: 'private-registry', url: 'https://registry.dev.zextras.com') {
            script {
              Set<String> imageTags = []

              if (env.BRANCH_NAME == 'devel') {
                imageTags.add('latest')
              } else if (buildingTag() && env.TAG_NAME?.trim()) {
                imageTags.add(env.TAG_NAME?.startsWith('v') ? env.TAG_NAME.substring(1) : env.TAG_NAME)
              } else if (params.PLAYGROUND == true) {
                imageTags.add(env.BRANCH_NAME.replaceAll('/', '-'))
              }

              dockerHelper.buildImage([
                imageName: 'registry.dev.zextras.com/dev/carbonio-ws-collaboration-ce',
                imageTags: imageTags,
                dockerfile: 'docker/wsc/Dockerfile',
                platforms: ['linux/amd64', 'linux/arm64'],
                ocLabels: [
                  title: 'Carbonio Ws Collaboration Community Edition',
                  descriptionFile: 'docker/wsc/description.md',
                  version: env.GIT_TAG ?: 'devel',
                ]
              ])
            }
          }
        }
      }
    }

    stage('Build deb/rpm') {
      steps {
        echo 'Building deb/rpm packages'
        buildStage([
          rockySinglePkg: true,
          ubuntuSinglePkg: true,
        ])
      }
    }

    stage('Upload artifacts') {
      steps {
        uploadStage(
          packages: yapHelper.getPackageNames(),
          rockySinglePkg: true,
          ubuntuSinglePkg: true,
        )
      }
    }
  }
}
