// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

pipeline {
  parameters {
    booleanParam defaultValue: false,
    description: 'Whether to upload the packages in playground repository',
    name: 'PLAYGROUND'
    booleanParam defaultValue: false,
    description: 'Whether to upload the packages in rc repository',
    name: 'RC'
  }
  options {
    skipDefaultCheckout()
    buildDiscarder(logRotator(numToKeepStr: '5'))
    timeout(time: 1, unit: 'HOURS')
  }
  agent {
    node {
      label 'openjdk11-agent-v1'
    }
  }
  environment {
    NETWORK_OPTS = '--network ci_agent'
    FAILURE_EMAIL_RECIPIENTS='smokybeans@zextras.com'
  }
  stages {
    stage('Build setup') {
      steps {
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
      }
    }
    stage('Compiling') {
      steps {
        sh 'mvn -T1C -B -q --settings settings-jenkins.xml compile'
      }
      post {
        failure {
          script {
            if ("main".equals(env.BRANCH_NAME)) {
              sendFailureEmail(STAGE_NAME)
            }
          }
        }
      }
    }
    stage('Testing') {
      steps {
        sh '''
          mvn -B --settings settings-jenkins.xml \
          -Dlogback.configurationFile="$(pwd)"/carbonio-ws-collaboration-boot/src/main/resources/logback-test-silent.xml \
          verify
        '''
        publishCoverage adapters: [jacocoAdapter('target/site/jacoco-all-tests/jacoco.xml')]
      }
      post {
        failure {
          script {
            if ("main".equals(env.BRANCH_NAME)) {
              sendFailureEmail(STAGE_NAME)
            }
          }
        }
      }
    }
    stage('Dependency check'){
      when {
         branch 'main'
      }
      steps {
        dependencyCheck additionalArguments: '''
          -o "./"
          -s "./"
          -f "HTML"
          --prettyPrint''', odcInstallation: 'dependency-check'
      }
    }
    stage('Sonarqube Analysis') {
      steps {
        withSonarQubeEnv(credentialsId: 'sonarqube-user-token', installationName: 'SonarQube instance') {
          sh '''
            mvn -Dsonar.dependencyCheck.htmlReportPath=./dependency-check-report.html \
            -Dsonar.coverage.jacoco.xmlReportPaths=../target/site/jacoco-all-tests/jacoco.xml \
            -B --settings settings-jenkins.xml sonar:sonar
          '''
        }
      }
    }
    stage('Stashing for packaging') {
      when {
        anyOf {
          branch "main"
          expression { params.PLAYGROUND == true }
        }
      }
      steps {
        stash includes: '**', name: 'project'
      }
    }
    stage('Building packages') {
      when {
        anyOf {
          branch "main"
          expression { params.PLAYGROUND == true }
        }
      }
      parallel {
        stage('Ubuntu') {
          agent {
            node {
              label 'yap-agent-ubuntu-20.04-v2'
            }
          }
          steps {
            unstash 'project'
            sh '''
              mkdir /tmp/ws-collaboration
              mv * /tmp/ws-collaboration
              sudo yap build ubuntu /tmp/ws-collaboration
            '''
            stash includes: 'artifacts/', name: 'artifacts-ubuntu'
          }
          post {
            failure {
              script {
                if ("main".equals(env.BRANCH_NAME)) {
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
              label 'yap-agent-rocky-8-v2'
            }
          }
          steps {
            unstash 'project'
            sh '''
              mkdir /tmp/ws-collaboration
              mv * /tmp/ws-collaboration
              sudo yap build rocky /tmp/ws-collaboration
            '''
            stash includes: 'artifacts/x86_64/*.rpm', name: 'artifacts-rocky'
          }
          post {
            failure {
              script {
                if ("main".equals(env.BRANCH_NAME)) {
                  sendFailureEmail(STAGE_NAME)
                }
              }
            }
            always {
              archiveArtifacts artifacts: 'artifacts/x86_64/*.rpm', fingerprint: true
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
          uploadSpec = '''{
            "files": [
              {
                "pattern": "artifacts/*.deb",
                "target": "ubuntu-playground/pool/",
                "props": "deb.distribution=focal;deb.distribution=jammy;deb.component=main;deb.architecture=amd64"
              },
              {
                "pattern": "artifacts/x86_64/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "centos8-playground/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras"
              },
              {
                "pattern": "artifacts/x86_64/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "rhel9-playground/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras"
              }
            ]
          }'''
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
        }
      }
    }
    stage('Upload To Devel') {
      when {
        branch "main"
      }
      steps {
        unstash 'artifacts-ubuntu'
        unstash 'artifacts-rocky'

        script {
          def server = Artifactory.server 'zextras-artifactory'
          def buildInfo
          def uploadSpec
          buildInfo = Artifactory.newBuildInfo()
          uploadSpec = '''{
            "files": [
              {
                "pattern": "artifacts/*.deb",
                "target": "ubuntu-devel/pool/",
                "props": "deb.distribution=focal;deb.distribution=jammy;deb.component=main;deb.architecture=amd64"
              },
              {
                "pattern": "artifacts/x86_64/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "centos8-devel/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras"
              },
              {
                "pattern": "artifacts/x86_64/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "rhel9-devel/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras"
              }
            ]
          }'''
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
        }
      }
      post {
        failure {
          script {
            if (env.BRANCH_NAME.equals("main")) {
              sendFailureEmail(STAGE_NAME)
            }
          }
        }
      }
    }
    stage('Upload To Release') {
      when {
        allOf {
          branch "main"
          expression { params.RC == true }
        }
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
          uploadSpec = '''{
            "files": [
              {
                "pattern": "artifacts/*.deb",
                "target": "ubuntu-rc/pool/",
                "props": "deb.distribution=focal;deb.distribution=jammy;deb.component=main;deb.architecture=amd64"
              }
            ]
          }'''
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
          uploadSpec = '''{
            "files": [
              {
                "pattern": "artifacts/x86_64/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "centos8-rc/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras"
              }
            ]
          }'''
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
          config = [
             'buildName'          : buildInfo.name,
             'buildNumber'        : buildInfo.number,
             'sourceRepo'         : 'centos8-rc',
             'targetRepo'         : 'centos8-rc',
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
          uploadSpec = '''{
            "files": [
              {
                "pattern": "artifacts/x86_64/(carbonio-ws-collaboration-ce)-(*).rpm",
                "target": "rhel9-rc/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras"
              }
            ]
          }'''
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
          config = [
             'buildName'          : buildInfo.name,
             'buildNumber'        : buildInfo.number,
             'sourceRepo'         : 'rhel9-rc',
             'targetRepo'         : 'rhel9-rc',
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
            if (env.BRANCH_NAME.equals("main")) {
              sendFailureEmail(STAGE_NAME)
            }
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
