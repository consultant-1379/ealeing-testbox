#!/usr/bin/env groovy

def call() {
    if(env.VERSANT && env.VERSANT=='true') {
        sh '''
        /proj/ciexadm200/tools/utils/versant/ci-versant_vApp.sh down -v ${VERSANT_DB_VERSION} -d ${VERSANT_DB_NAME}
        '''
    }
    stop_neo4j()
    changeReportPermissions()
    if(env.PUBLISH_SITE && env.PUBLISH_SITE=='true') {ci_pcr_site() }

    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
    archiveArtifacts artifacts: '**/*.log,**/*.ear,**/*.rpm', allowEmptyArchive: true
    read_send_metrics()
}

def stop_neo4j(){
    sh '''
    export MARKER=neo4j-java-driver-harness
    echo "Neo4j server PID: $(pgrep -f ${MARKER})"
    echo "Neo4j Server CMD:"
    ps auxwww | egrep "PID|${MARKER}" | grep -v grep
    #Stop  Neo4j db
    echo "Terminating Neo4j server"
    pkill -f ${MARKER} | true
    n=0
    while pgrep -f ${MARKER} ; do
        echo "Waiting for Neo4j termination ..."
        sleep 5
        ((n+=1))
        if [ $n -gt 10 ] ;then
            echo "Killing Neo4j server"
            pkill -9 -f ${MARKER} | true
            break
        fi
    done
    # Display Neo4j Server PID
    echo "Neo4j server PID: $(pgrep -f ${MARKER})"
    '''
}



def read_send_metrics() {
    """
    Function reads Jenkin's generated metrics in Jason format and sends them to grafana influx db.
    """
	if (env.SKIP_METRICS && env.SKIP_METRICS == 'true') {
        echo "Skipping metrics collection"
	}
	else{
        try {
            baseUrl = currentBuild.absoluteUrl.toString()
            buildInfoUrl=baseUrl+"wfapi/describe"
            jsonText = new URL(buildInfoUrl).text
            def parsedJson = readJSON text: jsonText
            def stageTimes = [:]
            stages = parsedJson['stages']
            println "----- Reading Stage time metrics  -------"
            for (int i = 0; i < stages.size(); i++) {
                stageName = stages[i]['name']
                stageTimes[stageName] = stages[i]['durationMillis']
            }
            //setting variable for inxlux db to monitore Quality gate status
            if (env.SKIP_SONAR && env.SKIP_SONAR == 'true') {
                stageTimes['gate'] = 1
                //collect metrics on the result of the SonarQube gate when its turned off
                if (env.qualityGate_status) {
                    if (env.qualityGate_status.replaceAll("\\s", "") != 'OK') {
                        stageTimes['QG_result_gateOff'] = 1
                    } else {
                        stageTimes['QG_result_gateOff'] = 0
                    }
                }
            }else{
                stageTimes['gate'] = 0
            }
            //collect metrics on pom configuration errors
            if (env.POM_CONFIG_ERROR && env.POM_CONFIG_ERROR == 'true') {
                stageTimes['pom_error'] = 1
            }else{
                stageTimes['pom_error'] = 0
            }

            //sending the collected metrics to inxlux db
            withSonarQubeEnv(env.SQ_SERVER) {
                step([$class: 'InfluxDbPublisher', target: 'pipeline', customPrefix: null, customData: stageTimes])
            }
        } catch(Exception exception) {
            echo "ERROR sending notification email for failure of generate_metrics."
            mail bcc: '', body: "<p><strong> The failure to send metrics to influx db! </strong><br /><br /><strong>Job</strong>: ${env.JOB_NAME} <br /><strong>Build Number</strong>: ${env.BUILD_NUMBER} <br /><strong>Build URL</strong>: ${env.BUILD_URL}", cc: '', charset: 'UTF-8', from: 'axisadm@fem142-eiffel004.lmera.ericsson.se', mimeType: 'text/html', replyTo: '', subject: "Failed to send metrics:  ${env.JOB_NAME}", to: "PDLCIAXISC@pdl.internal.ericsson.com"
        }
	}
}

def changeReportPermissions(){
	sh '[ -d ./testsuite ]&& sudo -A chown -R lciadm100:lciadm100 ./testsuite||true'
}
