import java.io.File

/*
loop createPipelinePreCodeUnitJob method for each entry of ci-jenkins-config/projects_unit file
e.g. about the file content:

OSS/com.ericsson.oss.services.shm/shm-common
OSS/com.ericsson.oss.services.shm/shm-licensemanagement
...

*/
String projectsFile = readFileFromWorkspace('ci-jenkins-config/projects_unit')
projectsFile.eachLine {
    project_name -> createPipelinePreCodeUnitJob(project_name)
}

String pipeline_name

def createPipelinePreCodeUnitJob(project_name) {
    println("Project name : " + project_name)
    def String ci_pipeline_jenkins_config_project = 'OSS/com.ericsson.oss.de/ci-pipeline-jenkins-config' // Pipeline official repository.
  
    // extract environment variables from pipeline.cfg located in development repository
    def cmd1='git archive --remote=ssh://gerrit.ericsson.se:29418/'+ci_pipeline_jenkins_config_project+' HEAD pipeline_global.cfg'
    def cmd2='tar --extract --to-stdout'
    def proc = cmd1.execute() | cmd2.execute()
    
    def std_out = new StringBuffer()
    proc.consumeProcessErrorStream(std_out)
    def config_file = proc.text
    
    /* check if groupID contains 'servicegroup' string,
    in order to avoid Pipeline job with same name
    */
    if (project_name.contains("servicegroup")) {
      pipeline_name = project_name.split('/').last() + "_sg_PCR"
    } else {
      pipeline_name = project_name.split('/').last() + "_PCR"
    }

    // Create or updates a pipeline job.
    pipelineJob(pipeline_name) {
        // jenkins job description.
        description('<b>PreCodeReview with Unit test - SQ Gate not enabled.<br>')
        
        // Block build if certain jobs are running.
        blockOn(project_name.split('/').last() + "_Release")

        // only up to this number of build records are kept.
        logRotator {
            numToKeep(20)
        }
        
        // Adds environment variables to the build.
        environmentVariables {
            env('REPO', project_name)
            config_file.eachLine {
            String line ->
                // environment variable name, environment variable value.
                def (env_name, env_value) = line.split('=',2)                
                env(env_name, env_value)
            }
        }
        
        // Sets the trigger strategy that Jenkins will use to choose what branches to build in what order.
        triggers {
            gerritTrigger {
                silentMode(true) // Sets silent mode to on or off. When silent mode is on there will be no communication back to Gerrit.
                triggerOnEvents {
                    patchsetCreated { // Trigger when a new change or patch set is uploaded.
                        excludeDrafts(false)
                        excludeTrivialRebase(false) // this will ignore any patchset which Gerrit considers a "trivial rebase" from triggering this build.
                        excludeNoCodeChange(false) // this will ignore any patchset which Gerrit considers without any code changes from triggering this build.
                    }
                }
                
                // Specify what Gerrit project(s) to trigger a build on.
                gerritProjects {
                    gerritProject {
                        compareType("PLAIN") // The exact repository name in Gerrit, case sensitive equality.
                        pattern(project_name)
                        branches {
                            branch {
                                compareType("PLAIN") // The exact branch name in Gerrit, case sensitive equality.
                                    pattern("master")
                            }
                        }
                        disableStrictForbiddenFileVerification(false)
                    }
                }
            }

            definition {
                // Specify where to obtain a source code repository containing your JenkinsFile.
                cps {
					script(readFileFromWorkspace('ci-jenkins-config/jenkinsFiles/ciPipelineJenkinsFiles/pcr.groovy'))
                }
            }
        }
    }
}

// Creates or updates a view that shows items in a simple list format.
listView('PCR') {
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex(/.*PCR/)
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}