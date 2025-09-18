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
    FAILURE_EMAIL_RECIPIENTS='smokybeans@zextras.com'
    NETWORK_OPTS = '--network ci_agent'
  }

  options {
    skipDefaultCheckout()
    buildDiscarder(logRotator(numToKeepStr: '5'))
    timeout(time: 1, unit: 'HOURS')
    parallelsAlwaysFailFast()
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
          checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            extensions: [[
              $class: 'CloneOption',
              shallow: true,
              depth:   2,
              timeout: 30
            ]],
            userRemoteConfigs: scm.userRemoteConfigs
          ])
          script {
            gitMetadata()
          }
        }
      }
    }

    stage('Compiling') {
      steps {
        container('jdk-17') {
          withCredentials([
            file(credentialsId: 'jenkins-maven-settings.xml', variable: 'SETTINGS_PATH')
          ]) {
            sh '''
              mvn -Dmaven.repo.local=$(pwd)/m2 -N wrapper:wrapper
              mvn -Dmaven.repo.local=$(pwd)/m2 -T1C -B -s $SETTINGS_PATH compile
              mvn package -Dmaven.main.skip -Dmaven.repo.local=$(pwd)/m2
              cp carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-ce-fatjar.jar package/
            '''
          }
        }
      }
      post {
        failure {
          script {
            if ("main".equals(BRANCH_NAME) || "devel".equals(BRANCH_NAME)) {
              sendFailureEmail(STAGE_NAME)
            }
          }
        }
      }
    }

    stage('Testing') {
      steps {
        container('jdk-17') {
          withCredentials([
            file(credentialsId: 'jenkins-maven-settings.xml', variable: 'SETTINGS_PATH')
          ]) {
            sh '''
              mvn -B --settings $SETTINGS_PATH \
              -Dlogback.configurationFile="$(pwd)"/carbonio-ws-collaboration-boot/src/main/resources/logback-test-silent.xml \
              verify
            '''
          }
          recordCoverage(tools: [[pattern: 'target/site/jacoco-all-tests/jacoco.xml']])
        }
      }
      post {
        failure {
          script {
            if ("main".equals(BRANCH_NAME) || "devel".equals(BRANCH_NAME)) {
              sendFailureEmail(STAGE_NAME)
            }
          }
        }
      }
    }

    stage('Sonarqube Analysis') {
      steps {
        container('jdk-17') {
          withCredentials([
            file(credentialsId: 'jenkins-maven-settings.xml', variable: 'SETTINGS_PATH')
          ]) {
            withSonarQubeEnv(credentialsId: 'sonarqube-user-token', installationName: 'SonarQube instance') {
              sh '''
                mvn -Dsonar.coverage.jacoco.xmlReportPaths=../target/site/jacoco-all-tests/jacoco.xml \
                -B --settings $SETTINGS_PATH sonar:sonar
              '''
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
      post {
        failure {
          script {
            if ("main".equals(BRANCH_NAME) || "devel".equals(BRANCH_NAME)) {
              sendFailureEmail(STAGE_NAME)
            }
          }
        }
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
      post {
        failure {
          script {
            if ("main".equals(BRANCH_NAME) || "devel".equals(BRANCH_NAME)) {
              sendFailureEmail(STAGE_NAME)
            }
          }
        }
      }
    }
  }
}

void sendFailureEmail(String step) {
  String commitInfo = sh(
     script: 'git log -1 --pretty=tformat:\'<ul><li>Revision: %H</li><li>Title: %s</li><li>Author: %ae</li></ul>\'',
     returnStdout: true
  )
  emailext body: """\
    <b>${step.capitalize()}</b> step has failed on trunk.<br /><br />
    Last commit info: <br />
    ${commitInfo}<br /><br />
    Check the failing build at the <a href=\"${BUILD_URL}\">following link</a><br />
  """,
  subject: "[WORKSTREAM COLLABORATION TRUNK FAILURE] Trunk ${step} step failure",
  to: FAILURE_EMAIL_RECIPIENTS
}
