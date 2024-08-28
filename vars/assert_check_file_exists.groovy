#!/usr/bin/env groovy

def call(fileName) {
    echo "looking for " +fileName
    if (fileExists(file: fileName)) {
        echo "Test passed "+ fileName + " in the root directory"
    } else {
        echo "Test failed: can't find " + fileName
        sh 'exit 1'
    }
}