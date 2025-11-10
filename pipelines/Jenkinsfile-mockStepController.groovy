pipeline {
    agent any // should be master/built-in
    stages {
        stage('Mixed Load') {
            steps {
                script {
                    mockLoad 10
                }
            }
        }
    }
}
