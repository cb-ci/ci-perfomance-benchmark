/**Requires two script approvals
 * method jenkins.model.Jenkins getLegacyInstanceId
 * staticMethod jenkins.model.Jenkins get
 **/
def instanceID=Jenkins.get().getLegacyInstanceId()
pipeline {
    agent none
    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
                //triggerRemoteJob mode: trackProgressAwaitResult(allowAbort: false, scheduledTimeout: [timeoutStr: ''], startedTimeout: [timeoutStr: ''], timeout: [timeoutStr: '1d'], whenFailure: stopAsFailure(), whenScheduledTimeout: continueAsIs(), whenStartedTimeout: continueAsIs(), whenTimeout: continueAsFailure(), whenUnstable: continueAsUnstable()), remotePathMissing: continueAsIs(), remotePathUrl: 'jenkins://b52f0c5313985cbde80b7602b63bcc51/test-triggers/child'
                triggerRemoteJob parameterFactories: [[$class: 'SimpleString', name: 'paramKey1', value: 'paramtValueFromparent']], remotePathMissing: stopAsFailure(), remotePathUrl: "jenkins://${instanceID}/test-triggers/child"
                //build 'child'
            }
        }
    }
}
