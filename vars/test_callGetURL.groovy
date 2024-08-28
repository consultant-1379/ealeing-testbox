#!/usr/bin/env groovy
import test_getWorkspaceURL

def call(){
    greet()
	call_getWS_url()
}


def greet() {
    println "Hello World!"
}

def call_getWS_url(){
t= new test_getWorkspaceURL()
t.getWorkspaceURL()
}
