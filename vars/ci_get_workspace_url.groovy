#!/usr/bin/env groovy

@NonCPS
def getWorkspaceURL() {
buildURL = BUILD_URL
counterOK=1;
echo "Build_URL:  $buildURL"
    for(stepID in 1..9){
        workspaceUrl = buildURL+"execution/node/"+stepID+"/ws/"
        try{
            url = new URL(workspaceUrl);
            def HttpURLConnection connection = url.openConnection()
            def getRC = connection.getResponseCode();
            if(getRC.equals(200)) {
                workspaceUrl200 = buildURL+"execution/node/"+"$stepID"+"/ws/"
                counterOK=2;
                break;
            } else{
                counterOK=1;
            }
        }catch (Exception e){
            println "Failure: Workspace URL not retrieved " + e
        }
    }
    if(counterOK==2) {
        manager.addBadge("terminal.gif", "workspace", "$workspaceUrl200")
        manager.createSummary("terminal.gif").appendText("<p> <b><a href=${workspaceUrl200}>Link to Workspace</p>", false, false, false, "red")
    }else {
        manager.createSummary("terminal.gif").appendText("<p>Something Wrong!!! Link to Workspace NOT generated<b></p>", false, false, false, "red")
    }
}

