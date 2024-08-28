#!/usr/bin/env groovy

def call() {
    sh "docker-compose  -f ${DOCKER_COMPOSE_FILE} up -d"
    sh "docker ps"
}

