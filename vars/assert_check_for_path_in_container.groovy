#!/usr/bin/env groovy

def call(container, filePath){
    def dockerCommand = "docker exec " + container + " ls " + filePath
    echo "Looking for " + filePath + " in container " + container
    def statusCode = sh script:"$dockerCommand", returnStatus:true
    if (statusCode == 0) {
        echo "** The artifact is successfully deployed"
    }else{
        echo "** Error the artifact is NOT deployed"
        sh 'exit 1'
    }
}
