pipeline {
  parameters {
    booleanParam defaultValue: false, description: 'Whether to build artifacts', name: 'ARTIFACTS'
    booleanParam defaultValue: false, description: 'Whether to upload the packages in playground repositories (needs artifacts parameter)', name: 'PLAYGROUND'
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
  }
  stages {
    stage('Build setup') {
      steps {
        checkout scm
        withCredentials([file(credentialsId: 'jenkins-maven-settings.xml', variable: 'SETTINGS_PATH')]) {
          sh 'cp $SETTINGS_PATH settings-jenkins.xml'
        }
      }
    }
    stage('Compiling') {
      steps {
        sh 'mvn -T1C -B -q --settings settings-jenkins.xml compile'
      }
    }
    stage('Testing') {
      steps {
        sh '''
        mvn -B --settings settings-jenkins.xml \
            -Dlogback.configurationFile="$(pwd)"/carbonio-chats-ce-boot/src/main/resources/logback-test-silent.xml \
            verify
        '''
      }
    }
    stage('Stashing for packaging') {
      when {
        anyOf {
          expression { params.ARTIFACTS == true }
        }
      }
      steps {
        stash includes: '**', name: 'project'
      }
    }
    stage('Building packages') {
      when {
        anyOf {
          expression { params.ARTIFACTS == true }
        }
      }
      parallel {
        stage('Ubuntu 20') {
          agent {
            node {
              label 'pacur-agent-ubuntu-20.04-v1'
            }
          }
          steps {
            unstash 'project'
            sh '''
              mkdir /tmp/chats
              mv * /tmp/chats
              sudo pacur build ubuntu-focal /tmp/chats
            '''
            stash includes: 'artifacts/', name: 'artifacts-ubuntu-focal'
          }
          post {
            always {
              archiveArtifacts artifacts: 'artifacts/*.deb', fingerprint: true
            }
          }
        }
        stage('Rocky 8') {
          agent {
            node {
              label 'pacur-agent-rocky-8-v1'
            }
          }
          steps {
            unstash 'project'
            sh '''
              mkdir /tmp/chats
              mv * /tmp/chats
              sudo pacur build rocky-8 /tmp/chats
            '''
            stash includes: 'artifacts/', name: 'artifacts-rocky-8'
          }
          post {
            always {
              archiveArtifacts artifacts: 'artifacts/*.rpm', fingerprint: true
            }
          }
        }
      }
    }
    stage('Upload To Playground') {
      when {
        allOf {
          expression { params.PLAYGROUND == true }
          expression { params.ARTIFACTS == true }
        }
      }
      steps {
        unstash 'artifacts-ubuntu-focal'
        unstash 'artifacts-rocky-8'
        script {
          def server = Artifactory.server 'zextras-artifactory'
          def buildInfo
          def uploadSpec
          buildInfo = Artifactory.newBuildInfo()
          uploadSpec = '''{
            "files": [
              {
                "pattern": "artifacts/*focal*.deb",
                "target": "ubuntu-playground/pool/",
                "props": "deb.distribution=focal;deb.component=main;deb.architecture=amd64"
              },{
                "pattern": "artifacts/(carbonio-videoserver-recorder)-(*).rpm",
                "target": "centos8-playground/zextras/{1}/{1}-{2}.rpm",
                "props": "rpm.metadata.arch=x86_64;rpm.metadata.vendor=zextras"
              }
            ]
          }'''
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
        }
      }
    }
  }
}