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
                ci_delivery_group_init()
            }
        }
        stage('Release') {
            steps {
                ci_full_release_build()
                ci_merge_to_master()
            }
        }
        stage('Delivery-Group') {
            when {
                environment name: 'CREATE_DELIVERY_GROUP', value: 'true'
            }
            steps {
                ci_create_delivery_group()
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
    post {
        always {
            ci_release_post()
        }
    }
}