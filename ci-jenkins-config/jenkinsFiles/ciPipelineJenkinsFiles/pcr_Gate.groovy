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
        stage('Sonar') {
            when {
                environment name: 'SKIP_SONAR_GLOBAL', value: 'false'
            }
            steps {
                ci_pcr_sonar_analysis()
            }
        }
        stage('Quality Gate') {
            when {
                environment name: 'SKIP_SONAR_GLOBAL', value: 'false'
            }
            steps {
                ci_pcr_get_qualitygate()
            }
        }
    }
    post {
        always {
            ci_pcr_post()
        }
    }
}
