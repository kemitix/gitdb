final String mvn = "mvn --batch-mode --update-snapshots --errors"
final dependenciesSupportJDK=9

pipeline {
    agent any
    stages {
        stage('master != SNAPSHOT') {
            // checks that the pom version is not a snapshot when the current or target branch is master
            when {
                expression {
                    (env.GIT_BRANCH == 'master' || env.CHANGE_TARGET == 'master') &&
                            (readMavenPom(file: 'pom.xml').version).contains("SNAPSHOT")
                }
            }
            steps {
                error("Build failed because SNAPSHOT version")
            }
        }
        stage('Build & Test') {
            steps {
                withMaven(maven: 'maven', jdk: 'JDK LTS') {
                    sh "${mvn} clean compile checkstyle:checkstyle pmd:pmd test"
                    //junit '**/target/surefire-reports/*.xml'
                    //sh "${mvn} jacoco:report com.gavinmogan:codacy-maven-plugin:coverage " +
                    //        "-DcoverageReportFile=target/site/jacoco/jacoco.xml " +
                    //        "-DprojectToken=`$JENKINS_HOME/codacy/token` " +
                    //        "-DapiToken=`$JENKINS_HOME/codacy/apitoken` " +
                    //        "-Dcommit=`git rev-parse HEAD`"
                    jacoco exclusionPattern: '**/*{Test|IT|Main|Application|Immutable}.class'
                    pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '', unHealthy: ''
                    step([$class: 'hudson.plugins.checkstyle.CheckStylePublisher',
                          pattern: '**/target/checkstyle-result.xml',
                          healthy:'20',
                          unHealthy:'100'])
                }
            }
        }
        stage('Verify & Install') {
            steps {
                withMaven(maven: 'maven', jdk: 'JDK LTS') {
                    sh "${mvn} -DskipTests install"
                }
            }
        }
        stage('SonarQube (github only)') {
            when { expression { env.GIT_URL.startsWith('https://github.com') } }
            steps {
                withSonarQubeEnv('sonarqube') {
                    withMaven(maven: 'maven', jdk: 'JDK LTS') {
                        sh "${mvn} org.sonarsource.scanner.maven:sonar-maven-plugin:3.4.0.905:sonar"
                    }
                }
            }
        }
        stage('Archiving') {
            when { expression { findFiles(glob: '**/target/*.jar').length > 0 } }
            steps {
                archiveArtifacts '**/target/*.jar'
            }
        }
        stage('Deploy (master on github)') {
            when { expression { (env.GIT_BRANCH == 'master' && env.GIT_URL.startsWith('https://github.com')) } }
            steps {
                withMaven(maven: 'maven', jdk: 'JDK LTS') {
                    sh "${mvn} deploy --activate-profiles release -DskipTests=true"
                }
            }
        }
        stage('Build Java 9') {
            when { expression { dependenciesSupportJDK >= 9 } }
            steps {
                withMaven(maven: 'maven', jdk: 'JDK 9') {
                    sh "${mvn} clean verify -Djava.version=9"
                }
            }
        }
        stage('Build Java 10') {
            when { expression { dependenciesSupportJDK >= 10 } }
            steps {
                withMaven(maven: 'maven', jdk: 'JDK 10') {
                    sh "${mvn} clean verify -Djava.version=10"
                }
            }
        }
    }
}
