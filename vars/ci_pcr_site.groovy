#!/usr/bin/env groovy

def call() {
    withMaven(jdk: env.JDK_HOME, maven: env.MVN_HOME, options: [junitPublisher(healthScaleFactor: 1.0)]) {
        sh "mvn site:site"

        if (env.GERRIT_PATCHSET_REVISION) {
          sh "mvn -Psite_review site:stage -DstagingDirectory=${WORKSPACE}/${GERRIT_PATCHSET_REVISION}"
        }
    }

    if (env.GERRIT_PATCHSET_REVISION) {
      sh '''
      #add comment to gerrit code review
      ssh -p 29418 gerrit.ericsson.se gerrit review ${GERRIT_PATCHSET_REVISION} -m '"Site:'${JOB_URL}' generated for Gerrit Patch Revision "'${GERRIT_PATCHSET_REVISION}
      '''

      // publish html
      // snippet generator doesn't include "target:"
      // https://issues.jenkins-ci.org/browse/JENKINS-29711.
      publishHTML (target: [
      allowMissing: false,
      alwaysLinkToLastBuild: false,
      keepAll: true,
      reportDir: '${GERRIT_PATCHSET_REVISION}',
      reportFiles: 'index.html',
      reportName: "Site Report"
      ])
    }

}
