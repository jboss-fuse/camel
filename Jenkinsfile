/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

def MAVEN_PARAMS = '-B -e -fae -V -Dmaven.repo.local=$WORKSPACE/.maven-repository -Dmaven.compiler.fork=true -Dsurefire.rerunFailingTestsCount=2 --no-transfer-progress -s $MAVEN_SETTINGS -Dnoassembly -Dnoarchetypes -Dnodocs'

pipeline {

    agent {
        label 'checkin'
    }

    tools {
        jdk 'java-1.8'
    }

    options {
        buildDiscarder(
            logRotator(artifactNumToKeepStr: '5', numToKeepStr: '10')
        )
        disableConcurrentBuilds()
    }

    stages {

        stage('Build') {
            steps {
                configFileProvider([configFile(fileId: 'fuse-maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    sh "./mvnw $MAVEN_PARAMS -Dmaven.test.skip.exec=true -Dinvoker.skip=true clean install"
                }
            }
        }

        stage('Checks') {
            steps {
                configFileProvider([configFile(fileId: 'fuse-maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    sh "./mvnw $MAVEN_PARAMS -Psourcecheck checkstyle:check"
                }
            }
            post {
                always {
                    recordIssues enabledForFailure: true, tool: checkStyle()
                }
            }
        }

        stage('Test') {
            steps {
                configFileProvider([configFile(fileId: 'fuse-maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    sh "./mvnw $MAVEN_PARAMS -Dmaven.test.failure.ignore=true test"
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml'
                }
            }
        }

    }

    post {
        always {
            recordIssues enabledForFailure: true, tools: [mavenConsole(), java(), javaDoc()]
            emailext(
                subject: '${DEFAULT_SUBJECT}',
                body: '${DEFAULT_CONTENT}',
                recipientProviders: [[$class: 'CulpritsRecipientProvider']]
            )
        }
    }
}
