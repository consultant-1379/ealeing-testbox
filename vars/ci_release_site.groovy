#!/usr/bin/env groovy

def call() {
    env.checkout_path = "${WORKSPACE}/target/checkout/pom.xml"

    if (fileExists(file: checkout_path)) {
        dir(env.WORKSPACE + '/target/checkout') {
            withMaven(jdk: env.JDK_HOME, maven: env.MVN_HOME, options: [junitPublisher(healthScaleFactor: 1.0)]) {
                sh "mvn site:site && mvn site:deploy && mvn -Psite_latest site:deploy"
            }
        }
    } else {
        println "Checkout_folder doesn't exist! Maven Site Documentation not generated."
    }
}