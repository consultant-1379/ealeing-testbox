@Library('ci-pipeline-lib') _
pipeline {
    agent { label env.SLAVE_LABEL }
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
        stage('Unit') {
            steps {
                ci_pcr_unit()
            }
        }
        stage('Integration') {
            steps {
                ci_pcr_integration_no_docker()
            }
        }
        /*stage('Sonar') {
            steps {
                ci_pcr_sonar_analysis()
            }
        }*/
    }
    post {
        always {
            ci_pcr_post()
        }
    }
}
