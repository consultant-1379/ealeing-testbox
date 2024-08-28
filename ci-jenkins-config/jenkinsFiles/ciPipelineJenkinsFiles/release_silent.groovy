@Library('ci-pipeline-lib') _
pipeline {
    agent { label 'GridEngine' }
    options {
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
    }
    stages {
        stage('Pre-step') {
            steps {
                ci_pcr_pre()
            }
        }
        stage('Clone') {
            steps {
                ci_pcr_clone()
            }
        }
        stage('Init') {
            steps {
                ci_pcr_init()
            }
        }
        stage('Sonar') {
            when {
                environment name: 'SKIP_SONAR_GLOBAL', value: 'false'
            }
            steps {
                ci_release_sonar()
            }
        }
    }
}
