#!/usr/bin/env groovy

import groovy.json.JsonSlurper

def call(){
    def deliverPackage_array = []

    pom = readMavenPom file: env.WORKSPACE + '/target/checkout/pom.xml'
    version = pom.version

    modules = pom.getModules()
    for(String module: modules) {
        matchRPMConvention = (module =~ /ERIC.*_CXP.*/)
        if (matchRPMConvention) {
            deliverPackage = module + "::" + version
            deliverPackage_array.add(deliverPackage)
        }
    }
    deliverPackage_list = deliverPackage_array.join("@@")
    matchRPMConvention = null
    pom = null

    env.PACKAGE_NAME = deliverPackage.tokenize('::')[0]
    env.PACKAGE_VERSION = version

    println deliverPackage_list

    if(env.AUTO_DELIVER == 'true'){
        env.MISSING_DEPENDENCIES = "false"
    } else {
        env.MISSING_DEPENDENCIES = "true"
    }

    comment = "Auto created by "+BUILD_URL+" "
    message = '{"creator": "lciadm100","product": "ENM","artifacts": "'+deliverPackage_list+'","jiraIssues": "'+env.JIRA+'","missingDependencies": "'+env.MISSING_DEPENDENCIES+'","comment":  "'+comment+'","team": "'+env.TEAM_NAME+'", "validateOnly": "false", "checkKgb":"False"}'

    println(message)

    try {
        post = new URL("https://cifwk-oss.lmera.ericsson.se/api/createDeliveryGroup/?format=json").openConnection();
        post.setRequestMethod("POST")
        post.setDoOutput(true)
        post.setRequestProperty("Content-Type", "application/json")
        post.getOutputStream().write(message.getBytes("UTF-8"));
        def postRC = post.getResponseCode();
        def jsonSlurper = new JsonSlurper();
        if(postRC.equals(201)) {
            env.DG_CREATED = "true"
            def groupIDJson = post.inputStream.text
            println(groupIDJson);
            env.DG_OUTPUT = groupIDJson
            def object = jsonSlurper.parseText(groupIDJson)
            group_id = object[0].deliveryGroup
            println(group_id)
            post = null
            postRC = null
            if(env.AUTO_DELIVER == 'true'){
                post = new URL("https://cifwk-oss.lmera.ericsson.se/api/autoCreatedGroupDeliver/?format=json").openConnection();
                message = '{"group_id": "'+group_id+'","user": "lciadm100"}'
                println(message)
                post.setRequestMethod("POST")
                post.setDoOutput(true)
                post.setRequestProperty("Content-Type", "application/json")
                post.setRequestProperty( 'Accept', 'application/json' )
                post.getOutputStream().write(message.getBytes("UTF-8"));
                postRC = post.getResponseCode();

                if(postRC.equals(200)) {
                    println(post.getInputStream().getText());
                }else{
                   errorMsg = post.getErrorStream().getText();
                   throw new Exception(errorMsg);
                }
            } else {
                println("Delivery group is only created but not Delivered automatically!!!")
            }
            post = null
            postRC = null
        } else {
            errorMsg = post.getErrorStream().getText();
            throw new Exception(errorMsg);
        }
        post = null
        postRC = null
    }
    catch (Exception e) {
        println "FAILURE: To post data into delivery Queue.Exception: " + e
        currentBuild.result = 'FAILURE'
    }
}