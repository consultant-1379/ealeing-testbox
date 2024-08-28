#!/usr/bin/env groovy
def call() {
    timeout(time: 5, unit: 'MINUTES') {
        withSonarQubeEnv(env.SQ_SERVER) {
            if (fileExists(file: 'target/sonar/report-task.txt')) {
                sh 'cat target/sonar/report-task.txt'
                def props = readProperties  file: 'target/sonar/report-task.txt'
                def (GROUPID, ARTIFACTID) = props['projectKey'].tokenize( ':' )
                env.DASHBOARD_URL =  props['serverUrl']+"/dashboard?branch="+props['branch']+"&id="+GROUPID+"%3A"+ARTIFACTID
                /*
                alternative to waitForQualityGate method to returns quality gate status.
                Error message:
                java.lang.IllegalStateException: Unable to get SonarQube task id and/or server name. Please use the 'withSonarQubeEnv' wrapper to run your analysis.
                */
								sh 'sleep 60' //TMP workaround for CIS-114386: sonarqube gate status can be fetched before analysis is complete
                def TaskUrl = props['ceTaskUrl']
                def TaskUrljson = sh(script: "curl -u b847e26dd8d10f274e19b6d2ad299a706e3ecff4: ${TaskUrl}", returnStdout: true)
                def analysisId = sh(script: "echo ${TaskUrljson} | grep -o -P '(?<=analysisId:).*?(?=})'", returnStdout: true)
                def analysisIdUrl = props['serverUrl']+"/api/qualitygates/project_status?analysisId="+analysisId
                def analysisIdjson = sh(script: "curl -u b847e26dd8d10f274e19b6d2ad299a706e3ecff4: ${analysisIdUrl}", returnStdout: true)
                env.qualityGate_status = sh(script: "echo ${analysisIdjson} | grep -o -P '(?<=status:).*?(?=})' | head -1", returnStdout: true)
            }
            if (env.GERRIT_CHANGE_NUMBER) {
                    qualityGateCheck()
            }
        }
    }
}
def qualityGateCheck() {
    if (env.SKIP_SONAR && env.SKIP_SONAR == 'true') {
        sh 'echo "SKIP_SONAR -> TRUE, disable Quality gate section"'
        env.SQ_MESSAGE="'"+"SonarQube Quality Gate: ${DASHBOARD_URL}"+"'"
        sh '''
        ssh -p 29418 gerrit.ericsson.se gerrit review --label 'SQ-Quality-Gate=0' --message ${SQ_MESSAGE} --project ${GERRIT_PROJECT} ${GERRIT_PATCHSET_REVISION}
        '''
        if (qualityGate_status.replaceAll("\\s","") != 'OK') {
            manager.addBadge("error.gif", "Patchset would have FAILED SQ Quality Gate threshold")
            manager.createSummary("error.gif").appendText("<h2>Patchset would have FAILED SQ Quality Gate threshold.</h2> <p>See <b><a href=${DASHBOARD_URL}> SQ report</a></b> for more details</p>", false, false, false, "red")
        }else {
            manager.addBadge("accept.png", "Patchset would have PASSED SQ Quality Gate threshold")
            manager.createSummary("accept.png").appendText("<h2>Nice! Patchset would have PASSED SQ Quality Gate threshold</h2>", false, false, false, "green")
        }
    } else {
        sh 'echo "SKIP_SONAR -> FALSE, proceeding to Quality Gate section"'
        if (qualityGate_status.replaceAll("\\s","") != 'OK') {
            env.SQ_MESSAGE="'"+"SonarQube Quality Gate Failed: ${DASHBOARD_URL}"+"'"
            sh '''
            ssh -p 29418 gerrit.ericsson.se gerrit review --label 'SQ-Quality-Gate=-1' --message ${SQ_MESSAGE} --project ${GERRIT_PROJECT} ${GERRIT_PATCHSET_REVISION}
            '''

            setQualityGateStatus()
            error "Pipeline aborted due to quality gate failure!\n Report: ${env.DASHBOARD_URL}\n Pom might be incorrectly defined for code coverage: https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?pageId=309793813"

        } else {
            env.SQ_MESSAGE="'"+"SonarQube Quality Gate Passed: ${DASHBOARD_URL}"+"'"
            sh '''
            ssh -p 29418 gerrit.ericsson.se gerrit review --label 'SQ-Quality-Gate=+1' --message ${SQ_MESSAGE} --project ${GERRIT_PROJECT} ${GERRIT_PATCHSET_REVISION}
            '''
        }
    }

}

def setQualityGateStatus() {
    manager.addWarningBadge("Pipeline aborted due to quality gate failure, see build summary for more information.")
    manager.createSummary("warning.gif").appendText("<h3>Pipeline aborted due to quality gate failure:</h3><ul><li>Please check the <a href=\"${env.DASHBOARD_URL}\">SonarQube Report</a></li><li>Please verify your POMs are configured according to: <a href=\"https://confluence-oss.seli.wh.rnd.internal.ericsson.com/pages/viewpage.action?pageId=309793813#SonarQube%22Howto%22forCI/CDPipeline-ConfiguringMaven/JaCoCoforJavaCodeCoverage\">SonarQube 'How to' for CI/CD Pipeline</a></li></ul>", false)
}


