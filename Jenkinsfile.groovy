final String mvn = "mvn --batch-mode --update-snapshots --errors"
final dependenciesSupportJDK = 9

pipeline {
    agent any
    stages {
        stage('release != SNAPSHOT') {
            // checks that the pom version is not a snapshot when the current or target branch is a release
            when {
                expression {
                    (startsWith(env.GIT_BRANCH, 'release') || startsWith(env.CHANGE_TARGET, 'release')) &&
                            (readMavenPom(file: 'pom.xml').version).contains("SNAPSHOT")
                }
            }
            steps {
                error("Build failed because SNAPSHOT version: [" + env.GIT_BRANCH + "][" + env.CHANGE_TARGET + "]")
            }
        }
        stage('Build & Test') {
            steps {
                withMaven(maven: 'maven', jdk: 'JDK LTS') {
                    sh "${mvn} clean compile checkstyle:checkstyle pmd:pmd test"
                    jacoco exclusionPattern: '**/*{Test|IT|Main|Application|Immutable}.class'
                    pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '', unHealthy: ''
                    step([$class   : 'hudson.plugins.checkstyle.CheckStylePublisher',
                          pattern  : '**/target/checkstyle-result.xml',
                          healthy  : '20',
                          unHealthy: '100'])
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
        stage('SonarQube (gitlab only)') {
            when { expression { env.GIT_URL.startsWith('https://gitlab.com') } }
            steps {
                withSonarQubeEnv('sonarqube') {
                    withMaven(maven: 'maven', jdk: 'JDK LTS') {
                        sh "${mvn} org.sonarsource.scanner.maven:sonar-maven-plugin:3.4.0.905:sonar"
                    }
                }
            }
        }
        stage('Deploy (release on gitlab)') {
            when {
                expression {
                    (env.GIT_BRANCH.startsWith('master') && env.GIT_URL.startsWith('https://gitlab.com'))
                }
            }
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

private boolean startsWith(String envSetting, String match) {
    envSetting != null && envSetting.startsWith(match)
}
