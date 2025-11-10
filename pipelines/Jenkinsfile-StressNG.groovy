// Jenkinsfile: High-Load Performance Pipeline
def yamlPod='''
              apiVersion: v1
              kind: Pod
              spec:                
                containers:
                - name: git
                  image: bitnami/git:latest
                  command:
                  - sleep
                  args:
                  - infinity
                  securityContext:
                    # ubuntu runs as root by default, it is recommended or even mandatory in some environments (such as pod security admission "restricted") to run as a non-root user.
                    runAsUser: 1000
                - name: stress-ng
                  # see https://colinianking.github.io/stress-ng/
                  image: ghcr.io/colinianking/stress-ng
                  command:
                  - sleep
                  args:
                  - infinity
                  securityContext:
                    # ubuntu runs as root by default, it is recommended or even mandatory in some environments (such as pod security admission "restricted") to run as a non-root user.
                    runAsUser: 1000
              '''
pipeline {
    agent none
    environment {
        //STRESS_NG_VERBOSE_METRICS="--timeout 60s --metrics-brief --verbose"
        STRESS_NG_VERBOSE_METRICS="--timeout 60s --metrics-brief"
    }
    stages {
        stage('Fan-out Load') {
            parallel {
                stage('Clone Heavy Repos') {
                    agent {
                        kubernetes {
                            yaml yamlPod
                            defaultContainer 'git'
                            retries 2
                        }
                    }
                    steps {
                        sh '''
                          mkdir -p workspace/repo && cd workspace/repo
                          git clone --depth=1  https://github.com/torvalds/linux.git .
                        '''
                    }
                }
                stage('Stress Pod Agent') {
                    agent {
                        kubernetes {
                            yaml yamlPod
                            defaultContainer 'stress-ng'
                            retries 2
                        }
                    }
                    steps {
                        //see https://colinianking.github.io/stress-ng/
                        sh '''
                          stress-ng --cpu 2 --io 2 --vm 2 --vm-bytes 256M  --sock 2 --hdd 1  ${STRESS_NG_VERBOSE_METRICS}  > metrics-agent.txt                           
                        '''
                        archiveArtifacts artifacts: 'metrics-agent.txt', followSymlinks: false
                    }
                }
                // This stage requires stress-ng in the controller pod container rather than in the agent context
                // It requires a custom image with stress-ng baked in
                stage('Stress Controller') {
                    agent {
                        //Run on master, requires 1 executor on Controller
                        label "built-in"
                    }
                    steps {
                        script {
                            // Configurable parameters
                            def cpuThreads = 2           // Number of threads to consume CPU
                            def memoryMB = 128         // Memory to allocate in MB
                            def runSeconds = 60          // Duration to run the stress in seconds

                            println "Starting stress test: CPU=${cpuThreads} threads, Memory=${memoryMB}MB, Duration=${runSeconds}s"

                            // Allocate memory
                            def allocatedMemory = []
                            def bytesPerMB = 1024 * 1024
                            (0..<memoryMB).each { i ->
                                allocatedMemory << new byte[bytesPerMB]
                            }
                            println "Allocated ${memoryMB}MB memory"

                            // Run CPU load in threads
                            def threads = (1..cpuThreads).collect {
                                Thread.start {
                                    def end = System.currentTimeMillis() + (runSeconds * 1000)
                                    while (System.currentTimeMillis() < end) {
                                        Math.sqrt(Math.random()) // Keep CPU busy
                                    }
                                }
                            }
                            //threads*.join()
                            println "Stress test complete."
                        }
                        //see https://colinianking.github.io/stress-ng/
                        /*
                        sh '''
                          stress-ng --cpu 2 --io 2 --vm 2 --vm-bytes 256M  --sock 2 --hdd 1  ${STRESS_NG_VERBOSE_METRICS}   > metrics-controller.txt                                  
                        '''
                        archiveArtifacts artifacts: 'metrics-controller.txt', followSymlinks: false
                        */
                    }
                }
                
                stage('Disk IO') {
                    agent {
                        kubernetes {
                            yaml yamlPod
                            defaultContainer 'stress-ng'
                            retries 2
                        }
                    }
                    /**
                     * IO on volume: How to get the metrics? From NodeExporter? Kubectmetrics?
                     *
                     */
                    steps {
                        sh '''
                          dd if=/dev/urandom of=largefile bs=1M count=500
                          md5sum largefile
                          pwd && ls -lah
                        '''
                        //Using stash/unstash is a bad practise, however, we use it here to create some IO between the agent and the controller volume (jenkins_home)
                        stash includes: 'largefile', name: 'myfile'
                        deleteDir()
                        unstash 'myfile'
                        sh '''
                          rm -f largefile
                        '''
                    }
                }
                /* TODO: adjust to your needs
                stage('Artifact Upload/Download') {
                    agent {
                        kubernetes {
                            yaml yamlPod
                            defaultContainer 'stress-ng'
                            retries 2
                        }
                    }
                    steps {
                        unstash 'myfile'
                        sh '''
                          curl -X PUT -T myfile http://artifact.repo/upload/myfile
                          curl -O http://artifact.repo/upload/myfile
                        '''
                    }
                }
                 */
                stage('Workspace Cleanup') {
                    agent {
                        kubernetes {
                            yaml yamlPod
                            defaultContainer 'built-in'
                            retries 2
                        }
                    }
                    steps {
                        deleteDir()
                    }
                }
            }
        }
    }
    post {
        always {
            echo "Test pipeline complete. Check metrics in Prometheus/Grafana."
        }
    }
}
