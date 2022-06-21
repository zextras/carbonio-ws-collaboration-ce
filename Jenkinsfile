pipeline {
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
    FAILURE_EMAIL_RECIPIENTS='alberto.pontini@zextras.com, luca.gasparini@zextras.com, noman.alishaukat@zextras.com'
  }
  stages {
    stage('Build setup') {
      steps {
        checkout scm
        withCredentials([file(credentialsId: 'jenkins-maven-settings.xml', variable: 'SETTINGS_PATH')]) {
          sh 'cp $SETTINGS_PATH settings-jenkins.xml'
          sh 'mvn -Dmaven.repo.local=$(pwd)/m2 -N wrapper:wrapper'
        }
      }
    }
    /* stage('Compiling') {
      steps {
        sh './mvnw -Dmaven.repo.local=$(pwd)/m2 -T1C -B -q --settings settings-jenkins.xml compile'
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
        ./mvnw -Dmaven.repo.local=$(pwd)/m2 -B --settings settings-jenkins.xml \
            -Dlogback.configurationFile="$(pwd)"/carbonio-chats-ce-boot/src/main/resources/logback-test-silent.xml \
            verify
        '''
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
    } */
    stage("Publishing documentation") {
      when {
        anyOf {
          //only if main
          expression { hasOpenAPIDocumentChanged() }
        }
      }
      steps {
        echo "upload!"
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
    stage('Stashing for packaging') {
      when {
        anyOf {
          branch "main"
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
              ./mvnw package -Dmaven.main.skip -Dmaven.repo.local=$(pwd)/m2 \
                -Dmaven.test.skip -P artifacts -D distro=ubuntu -DnoDocker
            '''
            stash includes: 'artifacts/', name: 'artifacts-ubuntu-focal'
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
        stage('Rocky 8') {
          agent {
            node {
              label 'pacur-agent-rocky-8-v1'
            }
          }
          steps {
            unstash 'project'
            sh '''
              ./mvnw package -Dmaven.main.skip -Dmaven.repo.local=$(pwd)/m2 \
                -Dmaven.test.skip -P artifacts -D distro=rocky-8 -DnoDocker
            '''
            stash includes: 'artifacts/', name: 'artifacts-rocky-8'
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
              archiveArtifacts artifacts: 'artifacts/*.rpm', fingerprint: true
            }
          }
        }
      }
    }
    stage('Upload To Devel') {
      when {
        branch "main"
      }
      steps {
        unstash 'artifacts-ubuntu-focal'
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
                  "props": "deb.distribution=focal;deb.component=main;deb.architecture=amd64"
              }
            ]
          }'''
          server.upload spec: uploadSpec, buildInfo: buildInfo, failNoOp: false
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
  subject: "[CHATS TRUNK FAILURE] Trunk ${step} step failure",
  to: FAILURE_EMAIL_RECIPIENTS
}

boolean hasOpenAPIDocumentChanged() {
  println sh(script: "git --version", returnStdout: true)
  println sh(script: "git --no-pager log -1 --name-only --exit-code", returnStdout: true)
  def isChanged = sh(
    script: "git --no-pager log -1 --name-only --exit-code | grep -x carbonio-chats-ce-openapi/src/main/resources/openapi/chats-api.yaml",
    returnStatus: true
  )
  return isChanged==0 ? true : false
}