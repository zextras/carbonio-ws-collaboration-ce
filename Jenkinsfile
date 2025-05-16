// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

pipeline {
  parameters {
    booleanParam defaultValue: false,
    description: 'Whether to upload the packages in playground repository',
    name: 'PLAYGROUND'
    booleanParam defaultValue: false,
    description: 'Whether to run the dependency check',
    name: 'DEPENDENCY_CHECK'
  }
  options {
    skipDefaultCheckout()
    buildDiscarder(logRotator(numToKeepStr: '5'))
    timeout(time: 1, unit: 'HOURS')
  }
  agent {
    node {
      label 'zextras-v1'
    }
  }
  environment {
    NETWORK_OPTS = '--network ci_agent'
    FAILURE_EMAIL_RECIPIENTS='smokybeans@zextras.com'
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
          withCredentials([file(credentialsId: 'jenkins-maven-settings.xml', variable: 'SETTINGS_PATH')]) {
            sh 'cp $SETTINGS_PATH settings-jenkins.xml'
            sh 'mvn -Dmaven.repo.local=$(pwd)/m2 -N wrapper:wrapper'
          }
          script {
            env.GIT_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
          }
        }
      }
    }
    stage('Compiling') {
      steps {
        container('jdk-17') {
          sh 'mvn -Dmaven.repo.local=$(pwd)/m2 -T1C -B -s settings-jenkins.xml compile'
          sh 'mvn package -Dmaven.main.skip -Dmaven.repo.local=$(pwd)/m2'
          sh 'mkdir project'
          sh 'cp -r package yap.json project'
          sh 'cp carbonio-ws-collaboration-boot/target/carbonio-ws-collaboration-ce-fatjar.jar project/package'
          stash includes: 'project/**', name: 'project'          
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
          sh '''
            mvn -B --settings settings-jenkins.xml \
            -Dlogback.configurationFile="$(pwd)"/carbonio-ws-collaboration-boot/src/main/resources/logback-test-silent.xml \
            verify
          '''
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
          withSonarQubeEnv(credentialsId: 'sonarqube-user-token', installationName: 'SonarQube instance') {
            sh '''
              mvn -Dsonar.coverage.jacoco.xmlReportPaths=../target/site/jacoco-all-tests/jacoco.xml \
              -B --settings settings-jenkins.xml sonar:sonar
            '''
          }
        }
      }
    }
    stage('Building packages') {
      parallel {
        stage('Ubuntu') {
          agent {
            node {
              label 'yap-ubuntu-20-v1'
            }
          }
          steps {
            container('yap') {
              unstash 'project'
              script {
                if (BRANCH_NAME == 'devel') {
                  def timestamp = new Date().format('yyyyMMddHHmmss')
                  sh "sudo yap build ubuntu project -r ${timestamp}"
                } else {
                  sh 'sudo yap build ubuntu project'
                }
              }
              stash includes: 'artifacts/', name: 'artifacts-ubuntu'
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
            always {
              archiveArtifacts artifacts: 'artifacts/*.deb', fingerprint: true
            }
          }
        }
        stage('RHEL') {
          agent {
            node {
              label 'yap-rocky-8-v1'
            }
          }
          steps {
            container('yap') {
              unstash 'project'
              script {
                if (BRANCH_NAME == 'devel') {
                  def timestamp = new Date().format('yyyyMMddHHmmss')
                  sh "sudo yap build rocky project -r ${timestamp}"
                } else {
                  sh 'sudo yap build rocky project'
                }
              }
              stash includes: 'artifacts/*.rpm', name: 'artifacts-rocky'
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
            always {
              archiveArtifacts artifacts: 'artifacts/*.rpm', fingerprint: true
            }
          }
        }
      }
    }
    stage('Upload To Playground') {
      when {
        expression { params.PLAYGROUND == true }
      }
      steps {
        unstash 'artifacts-ubuntu'
        unstash 'artifacts-rocky'

        script {
          def server = Artifactory.server 'zextras-artifactory'
          def buildInfo
          def uploadSpec
          buildInfo = Artifactory.newBuildInfo()
          uploadSpec = """{
            "files": [
              {
                "pattern": "artifacts/*.deb",
                "target": "ubuntu-playground/pool/",
                "props": "deb.distribution=focal;deb.distribution=jammy;deb.distribution=noble;deb.component=main;deb.architecture=amd64;vcs.revision=${env.GIT_COMMIT}"
              },
              {
                "pattern": "artifacts/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "centos8-playground/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras;vcs.revision=${env.GIT_COMMIT}"
              },
              {
                "pattern": "artifacts/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "rhel9-playground/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras;vcs.revision=${env.GIT_COMMIT}"
              }
            ]
          }"""
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
        }
      }
    }
    stage('Upload To Devel') {
      when {
        branch 'devel'
      }
      steps {
        unstash 'artifacts-ubuntu'
        unstash 'artifacts-rocky'

        script {
          def server = Artifactory.server 'zextras-artifactory'
          def buildInfo
          def uploadSpec
          buildInfo = Artifactory.newBuildInfo()
          uploadSpec = """{
            "files": [
              {
                "pattern": "artifacts/*.deb",
                "target": "ubuntu-devel/pool/",
                "props": "deb.distribution=focal;deb.distribution=jammy;deb.distribution=noble;deb.component=main;deb.architecture=amd64;vcs.revision=${env.GIT_COMMIT}"
              },
              {
                "pattern": "artifacts/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "centos8-devel/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras;vcs.revision=${env.GIT_COMMIT}"
              },
              {
                "pattern": "artifacts/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "rhel9-devel/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras;vcs.revision=${env.GIT_COMMIT}"
              }
            ]
          }"""
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
        }
      }
      post {
        failure {
          script {
            sendFailureEmail(STAGE_NAME)
          }
        }
      }
    }
    stage('Upload & Promotion Config') {
      when {
        buildingTag()
      }
      steps {
        unstash 'artifacts-ubuntu'
        unstash 'artifacts-rocky'

        script {
          def server = Artifactory.server 'zextras-artifactory'
          def buildInfo
          def uploadSpec
          def config

          //ubuntu
          buildInfo = Artifactory.newBuildInfo()
          buildInfo.name += '-ubuntu'
          uploadSpec = """{
            "files": [
              {
                "pattern": "artifacts/*.deb",
                "target": "ubuntu-rc/pool/",
                "props": "deb.distribution=focal;deb.distribution=jammy;deb.distribution=noble;deb.component=main;deb.architecture=amd64;vcs.revision=${env.GIT_COMMIT}"
              }
            ]
          }"""
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
          config = [
             'buildName'          : buildInfo.name,
             'buildNumber'        : buildInfo.number,
             'sourceRepo'         : 'ubuntu-rc',
             'targetRepo'         : 'ubuntu-release',
             'comment'            : 'Do not change anything! Just press the button',
             'status'             : 'Released',
             'includeDependencies': false,
             'copy'               : true,
             'failFast'           : true
          ]
          Artifactory.addInteractivePromotion server: server,
          promotionConfig: config,
          displayName: 'Ubuntu Promotion to Release'
          server.publishBuildInfo buildInfo

          //rhel8
          buildInfo = Artifactory.newBuildInfo()
          buildInfo.name += "-centos8"
          uploadSpec = """{
            "files": [
              {
                "pattern": "artifacts/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "centos8-rc/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras;vcs.revision=${env.GIT_COMMIT}"
              }
            ]
          }"""
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
          config = [
             'buildName'          : buildInfo.name,
             'buildNumber'        : buildInfo.number,
             'sourceRepo'         : 'centos8-rc',
             'targetRepo'         : 'centos8-release',
             'comment'            : 'Do not change anything! Just press the button',
             'status'             : 'Released',
             'includeDependencies': false,
             'copy'               : true,
             'failFast'           : true
          ]
          Artifactory.addInteractivePromotion server: server,
          promotionConfig: config,
          displayName: 'RHEL8 Promotion to Release'
          server.publishBuildInfo buildInfo

          //rhel9
          buildInfo = Artifactory.newBuildInfo()
          buildInfo.name += "-rhel9"
          uploadSpec = """{
            "files": [
              {
                "pattern": "artifacts/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "rhel9-rc/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras;vcs.revision=${env.GIT_COMMIT}"
              }
            ]
          }"""
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
          config = [
             'buildName'          : buildInfo.name,
             'buildNumber'        : buildInfo.number,
             'sourceRepo'         : 'rhel9-rc',
             'targetRepo'         : 'rhel9-release',
             'comment'            : 'Do not change anything! Just press the button',
             'status'             : 'Released',
             'includeDependencies': false,
             'copy'               : true,
             'failFast'           : true
          ]
          Artifactory.addInteractivePromotion server: server,
          promotionConfig: config,
          displayName: 'RHEL9 Promotion to Release'
          server.publishBuildInfo buildInfo
        }
      }
      post {
        failure {
          script {
            sendFailureEmail(STAGE_NAME)
          }
        }
      }
    }
  }
}

void sendFailureEmail(String step) {
  def commitInfo =sh(
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
