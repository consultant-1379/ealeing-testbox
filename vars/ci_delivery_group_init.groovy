#!/usr/bin/env groovy
import groovy.json.JsonSlurper

def call() {
    setJira()
    setAutoDg()
    setTeamName()
}

def setTeamName() {
    if (env.JIRA != '' && !env.JIRA.toUpperCase().contains('NO JIRA')) {
        def getTeamNameURL = "https://cifwk-oss.lmera.ericsson.se/api/getteamfromjira/number/" + env.JIRA + "/?format=json"
        try {
            jsonText = new URL(getTeamNameURL).text
            env.TEAM_NAME = new JsonSlurper().parseText(jsonText).team
        }
        catch (Exception e) {
            println "no team"
        }
    }
    if (env.CREATE_DELIVERY_GROUP == "true") {
        if (!env.TEAM_NAME) {
            if (env.DEFAULT_TEAM) {
                env.TEAM_NAME = env.DEFAULT_TEAM
            } else {
                error "DEFAULT_TEAM variable is not defined. So delivery group cannot be created and delivered automatically!"
            }
        }
    }
}

def setJira() {
    def JiraMatcher = null
    if (!env.GERRIT_CHANGE_SUBJECT) {
        println "GERRIT_CHANGE_SUBJECT does not exist. Proceed to get last commit message from master branch history..."
        env.COMMIT_MESSAGE = sh(script: "git log --format='%H %an' | grep -Eiv 'admin|jenkins|self' | head -1 | cut -d ' ' -f1 | xargs -n1 git log --format='%s' -1", returnStdout: true)

        println env.COMMIT_MESSAGE

        if (COMMIT_MESSAGE =~ /(?i)(TORF-\d+(?!\d+))/) {
            JiraMatcher = (COMMIT_MESSAGE =~ /(?i)(TORF-\d+(?!\d+))/) // extract TORF reference in commit message
        }
        if (JiraMatcher) {
            env.JIRA = JiraMatcher[0][1]
        } else {
            env.JIRA = 'NO JIRA'
        }
    }
    JiraMatcher = null
}

def setAutoDg() {
    if (env.GERRIT_CHANGE_SUBJECT) {
        if (env.GERRIT_CHANGE_SUBJECT.toUpperCase().contains("AUTO_DELIVER")) {
            env.CREATE_DELIVERY_GROUP = "true"
            env.AUTO_DELIVER = "true"
        }
    } else {
        if (env.COMMIT_MESSAGE) {
            if (env.COMMIT_MESSAGE.toUpperCase().contains("AUTO_DELIVER")) {
                env.CREATE_DELIVERY_GROUP = "true"
                env.AUTO_DELIVER = "true"
            }
        }
    }
}