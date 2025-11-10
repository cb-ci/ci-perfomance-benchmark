pipeline {
    agent none
    parameters {
        string defaultValue: 'paramValue1', description: 'paramKey1=paramValue1', name: 'paramKey1', trim: true
    }
    stages {
        stage('Hello') {
            steps {
                echo "Hello Child Parameters: paramKey1=${params.paramKey1}"
                //sleep 1000
            }
        }
    }
}
