import java.io.File

/*
loop createPipelineReleaseJob method for each entry of ci-jenkins-config/projects file
e.g. about the file content:

OSS/com.ericsson.oss.services.shm/shm-common
OSS/com.ericsson.oss.services.shm/shm-licensemanagement
...

*/
projectFileLocation = 'ci-jenkins-config/projects'
String projectsFile = readFileFromWorkspace(projectFileLocation)

jenkins_instance = DOMAIN_ID.tokenize('.').last()
def env_map = [:] // hashmap to group environment variables contained in projects file
String pipeline_name

def list_of_jobs = [] // define list of already existing Release jobs
try {
    hudson.model.Hudson.instance.getView('Release').items.each() {
        list_of_jobs << it.fullDisplayName
    }
} catch (Exception ex) {
    return null;
}

projectsFile.eachLine {
    project_details ->
        if (project_details.contains(jenkins_instance) && !project_details.contains("EXC-Release")){
            project_name = project_details.tokenize(',')[0]
            project_name = project_name.replaceAll("\\s","") // remove empty spaces

            project_details.tokenize(',').each {
                project_element ->
                    if (project_element.contains("=")) {
                        def project_variable_name = project_element.tokenize('=')[0]
                        def project_variable_value = project_element.tokenize('=')[1]
                        env_map[project_variable_name] = project_variable_value
                    }
            }

            /* check if groupID contains 'servicegroup' string,
            in order to avoid Pipeline job with same name
            */
            if (project_name.contains("servicegroup")) {
                pipeline_name = project_name.split('/').last() + "_sg_Release"
            } else {
                pipeline_name = project_name.split('/').last() + "_Release"
            }

            /* handle different DSL's trigger mechanisms,
            automatic or manual
            */
            try { // in case of PCR triggered by a code review (automatically) only not already existing jobs will be considered
                println "GERRIT_PATCHSET_REVISION: " + GERRIT_PATCHSET_REVISION
                if (!(pipeline_name in list_of_jobs)) {
                    createPipelineReleaseJob(project_name,pipeline_name,env_map,"true")
                }
            } catch(Exception ex) { // in case of PCR triggered by an user (manually) only repositories in SILENT mode will be considered  
                if (project_details.contains("SILENT_MODE=true")) {
                    createPipelineReleaseJob(project_name,pipeline_name,env_map,"false")
                }
            }
        }
}

def createPipelineReleaseJob(project_name,pipeline_name,env_map,automatic_trigger) {
    println("Project name : " + project_name)
    def String ci_pipeline_jenkins_config_project = 'OSS/com.ericsson.oss.de/ci-pipeline-jenkins-config' // Pipeline official repository.

    // extract environment variables from pipeline.cfg located in development repository
    def cmd1='git archive --remote=ssh://gerrit.ericsson.se:29418/'+ci_pipeline_jenkins_config_project+' HEAD pipeline_global.cfg'
    def cmd2='tar --extract --to-stdout'
    def proc = cmd1.execute() | cmd2.execute()

    def std_out = new StringBuffer()
    proc.consumeProcessErrorStream(std_out)
    def config_file = proc.text

    // Create or updates a pipeline job.
    pipelineJob(pipeline_name) {
        // jenkins job description.
        description('<p>Job in "Silent Mode" - an imitation of a release job - it generates baseline to SonarQube. Code coverage and Unit tests are ran again.</p>')

        // Block build if certain jobs are running.
        blockOn(project_name.split('/').last() + "_PCR")

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
                    def (env_name, env_value) = line.split('=', 2)

                    if (env_name.equals("SLAVE_LABEL")) {
                       env_value = env_value.replaceAll("FEM",jenkins_instance)
                    }

                    if (env_map.containsKey(env_name)) {
                        env(env_name, env_map[env_name])
                        return //return acts like continue in normal loop
                    }

                    env(env_name, env_value)
            }
        }

        // Sets the trigger strategy that Jenkins will use to choose what branches to build in what order.
        triggers {
            definition {
                // Specify where to obtain a source code repository containing your JenkinsFile.
                cps {
                    script(readFileFromWorkspace('ci-jenkins-config/jenkinsFiles/ciPipelineJenkinsFiles/release_silent.groovy'))
                    sandbox(true)
                }
            }
        }
    }

    if (automatic_trigger.equals("true")) {
        queue(pipeline_name)
    }
}

// Creates or updates a view that shows items in a simple list format.
listView('Release') {
    filterBuildQueue()
    filterExecutors()
    jobs {
        regex(/.*Release/)
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