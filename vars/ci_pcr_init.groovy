#!/usr/bin/env groovy
import groovy.json.JsonSlurper
import ci_get_workspace_url

def call() {
    setEnvironmentVariables()
    setJira()
    setTeamName()
    setVersionFromPom()
    setSonarBranch()
    setBuildName()
    sonarSkipCheck()
    checkRootPom()
    checkModulePoms()
    postWorkspaceURL()
    //setPomStatus()
}

def setTeamName() {
    if (env.JIRA != '' && !env.JIRA.toUpperCase().contains('NO JIRA')) {
        def getTeamNameURL = "https://cifwk-oss.lmera.ericsson.se/api/getteamfromjira/number/" + env.JIRA + "/?format=json"
        try {
            jsonText = new URL(getTeamNameURL).text
            TEAM_NAME = new JsonSlurper().parseText(jsonText).team
        }
        catch (Exception e) {
            println "no team"
        }
    }
}

def setBuildName() {
    currentBuild.displayName = "${BUILD_NUMBER} | ${env.SONAR_BRANCH} | ${TEAM_NAME}"
}

def setJira() {
    def JiraMatcher = null
    if (env.GERRIT_CHANGE_SUBJECT) {
        if (GERRIT_CHANGE_SUBJECT =~ /(?i)(TORF-\d+(?!\d+))/) {
            JiraMatcher = (GERRIT_CHANGE_SUBJECT =~ /(?i)(TORF-\d+(?!\d+))/) // extract TORF reference in commit message
        }
        if (JiraMatcher) {
            env.JIRA = JiraMatcher[0][1]
        } else {
            env.JIRA = 'NO JIRA'
        }
    } else {
        env.JIRA = ''
    }
    JiraMatcher = null
}

def setSonarBranch() {
    if (env.GERRIT_CHANGE_SUBJECT) {
        if (env.JIRA != '' && !env.JIRA.toUpperCase().contains('NO JIRA')) {
            env.SONAR_BRANCH = env.JIRA
        } else {
            env.SONAR_BRANCH = 'No-Jira-' + GERRIT_CHANGE_NUMBER
        }
    } else {
        env.SONAR_BRANCH = env.VERSION 
    }
}

def setVersionFromPom() {
    pom = readMavenPom file: 'pom.xml'
    env.VERSION = pom.version
}

//        envVariableMap.each { entry -> env."$entry.key" = "$entry.value" }

def setEnvironmentVariables() { 

// Users pipeline_local.cfg is checked ONLY when env.ADMIN_CONTROL = FALSE  ( that is the default value )
// Modification is done at any JJob INIT step and at WORKSPACE level ONLY ( no pipeline_local.cfg is modified at all )
// All variables set by users and ignored here, are managed as per their pipeline_global.cfg default value

    if ((env.ADMIN_CONTROL != 'true')&&(fileExists(file: 'pipeline_local.cfg'))) {

    // keep ONLY variables (from pipeline_local.cfg) NOT included into fixed_envvars array, and map them as env vars.  
       def envVariableMap = readProperties  file: 'pipeline_local.cfg'
       def String[] fixedvar = ['ADMIN_CONTROL', 'SKIP_SONAR', 'MVN_SONAR']
       println " SKIP_SONAR global value is : $env.SKIP_SONAR "
       envVariableMap.each { entry ->
                if (!fixedvar.contains("$entry.key")) {
                   env."$entry.key" = "$entry.value"
                   println "ACCEPTED ENV VAR FOUND IN PIPELINE_LOCAL.CFG : $entry.key = $entry.value"
                }  else {
                   String defaultVar = env["$entry.key"]
                   println " IGNORED $entry.key VAR IN PIPELINE_LOCAL.CFG, DEFAULTED TO  $defaultVar "
                   }
       }           
   }
}

def sonarSkipCheck() {
    echo "Checking Sonar Global Skip"
    if (env.SKIP_SONAR_GLOBAL == 'true'){
        echo "Sonar Global Skip Enabled"
    } else {
        env.SKIP_SONAR_GLOBAL = 'false'
        echo "Sonar Global Skip disabled"
    }
}

def checkRootPom() {
    try {
        env.ROOT_POM_MAVEN_SUREFIRE = false
        env.ROOT_POM_MAVEN_JOCOCO = false
        def pom = readFile('pom.xml')

        pom = pom.replace("\uFEFF", "")//remove BOM header from file
        def config = new XmlParser().parseText(pom)


        for (item in config.build.plugins.plugin) {
            pluginCheck(item)
         }


        for (item in config.build.pluginManagement.plugins.plugin) {
            pluginCheck(item)
        }
    } catch (Exception exception) {
        echo "ERROR processing pom:"+exception
    }
}

def pluginCheck(item){
    if (item.artifactId.text() == 'maven-surefire-plugin') {
            env.ROOT_POM_MAVEN_SUREFIRE = true
        }

        if (item.artifactId.text() == 'jacoco-maven-plugin') {
            if (item.executions.execution.goals.goal.text().contains('prepare-agent') && item.executions.execution.goals.goal.text().contains('report')) {
                env.ROOT_POM_MAVEN_JOCOCO = true
        }
    }
}

def checkModulePoms() {
    try {
        env.MODULE_POM_MAVEN_SUREFIRE = false
        def files = findFiles(glob: '**/pom.xml')
        for (pomFile in files) {
            println pomFile
            checkModulePom(pomFile)
        }
    } catch (Exception exception) {
        echo "ERROR processing pom:"+exception
    }
}

def checkModulePom(pomLocation) {
    try {
        def pom = readFile(pomLocation.getPath())
        pom = pom.replace("\uFEFF", "")//remove BOM header from file
        def config = new XmlParser().parseText(pom)

        for (item in config.build.plugins.plugin) {

            if (item.artifactId.text() == 'maven-surefire-plugin') {
                if (item.configuration.includes != []) {
                    env.MODULE_POM_MAVEN_SUREFIRE = true
                }
            }
        }
    } catch (Exception exception) {
        echo "ERROR processing pom:"+exception
    }
}

def setPomStatus() {
    try {
        if (env.ROOT_POM_MAVEN_SUREFIRE == 'false' || env.ROOT_POM_MAVEN_JOCOCO == 'false' || env.MODULE_POM_MAVEN_SUREFIRE == 'false') {
            manager.addWarningBadge("Pom might be incorrectly defined for code coverage, See build summary for more information.")
            manager.createSummary("warning.gif").appendText("<h3>Pom might be incorrectly defined for code coverage:</h3><ul><li>If code coverage is calculated correctly, please IGNORE this warning </li><li>Otherwise see page:<a href=\"https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/CICD/How+to+Configure+Unit+Test+Code+Coverage+Reports+with+the+JaCoCo+Maven+Plugin+for+SonarQube\">How to Configure Unit Test Code Coverage Reports with the JaCoCo Maven Plugin for SonarQube</a></li></ul>", false)
            env.POM_CONFIG_ERROR = 'true'
        }
    } catch (Exception exception) {
        echo "ERROR processing pom:"+exception
    }
}

def postWorkspaceURL(){
    t= new ci_get_workspace_url()
    t.getWorkspaceURL()
}
