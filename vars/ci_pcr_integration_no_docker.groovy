#!/usr/bin/env groovy

def call() {
	killJbossProcess()
	runIntegrationTest()
}

def killJbossProcess() {
   sh 'pkill -9 -f jboss | true'
}

def runIntegrationTest() {
    if (env.MVN_PCR_INT) {
        env.MAVEN_COMMAND =  env.MVN_PCR_INT
    } else {
        env.MAVEN_COMMAND = "-V -U jacoco:prepare-agent install jacoco:report pmd:pmd"
    }

    if(env.VERSANT && env.VERSANT=='true') {
        sh '''
        /proj/ciexadm200/tools/utils/versant/ci-versant_vApp.sh up -v ${VERSANT_DB_VERSION} -d ${VERSANT_DB_NAME}
        '''
        env.VERSANT_ROOT = sh(script: 'echo "/proj/${USER}/tools/versant/$(hostname -s)/${VERSANT_DB_VERSION}"', returnStdout: true)
    }
    if(env.NEO4J && env.NEO4J=='true') {
        start_neo4j()
    }
    withMaven(jdk: env.JDK_HOME, maven: env.MVN_HOME, options: [junitPublisher(healthScaleFactor: 1.0)]) {
        sh "mvn ${MAVEN_COMMAND}"
    }
}

def start_neo4j() {
    //if neo4j version is defined in pom use that one
    check_pom_for_neo4j_version()
    //if version not defined in pom or local pipeline file, then get latest from nexus
    if (!env.NEO4J_VERSION) {
        get_latest_neo4j_version()
    }

    sh '''
    export NEO4J_DIR=/proj/ciexadm200/tools/neo4j-java-driver-harness/
    export NEO4J_HARNESS_JAR=${NEO4J_DIR}/neo4j-java-driver-harness-${NEO4J_VERSION}-shaded.jar
    [ -d ${NEO4J_DIR} ] || mkdir ${NEO4J_DIR}
    if [ ! -f "${NEO4J_HARNESS_JAR}" ]; then
        echo -e ">>> Downloading Neo4j Server from Nexus"
        export nexusServer="https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/repositories/public/com/ericsson/oss/itpf/datalayer/dps/3pp"
        curl -o ${NEO4J_HARNESS_JAR} ${nexusServer}/neo4j-java-driver-harness/${NEO4J_VERSION}/neo4j-java-driver-harness-${NEO4J_VERSION}-shaded.jar
    fi
    echo -e ">>> Starting Neo4j Server"
    java -jar ${NEO4J_HARNESS_JAR} > server.log 2>&1 &
    n=0
    while ! netstat -tunlp 2> /dev/null  | grep 7474 ; do
       echo -e ">>> Wating for Neo4j server to start ..."
       sleep 10
       if [ $n -gt 40 ] ;then
          echo -e "ERROR: Neo4j server didn't start up in given time"
          exit 1
       fi
       n=$((n+1))
    done
    echo -e ">>> Neo4j Server Started with PID: $(pgrep -f neo4j-java-driver-harness)"
    echo -e ">>> Neo4j Server CMD:"
    ps auxwww | egrep "PID|neo4j-java-driver-harness" | grep -v grep
    echo -e "\n>>> Checking Neo4j Server REST interface end points"
    curl -X GET -H "Accept: application/json" -H "Content-Type: application/json" http://localhost:7474/db/data/
    echo
    '''
}

def check_pom_for_neo4j_version() {
    if (fileExists("data-persistence-service-bom/pom.xml")) {
        env.NEO4J_VERSION = sh(script: 'grep -oPm1 "(?<=<version.neo4j.impl.3pp>)[^<]+" data-persistence-service-bom/pom.xml', returnStdout: true)
        env.NEO4J_VERSION = env.NEO4J_VERSION.replaceAll("\\s", "")
    }
}

def get_latest_neo4j_version() {
    println "NEO4J_VERSION not defined, using latest defined in nexus"
    getLatestNeo4JUrl = "https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/repositories/public/com/ericsson/oss/itpf/datalayer/dps/3pp/neo4j-java-driver-harness/maven-metadata.xml"
    mavenMetadata = new XmlSlurper().parse(getLatestNeo4JUrl)
    env.NEO4J_VERSION = mavenMetadata.versioning.latest
    mavenMetadata = null
}
