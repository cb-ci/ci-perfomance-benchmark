def REPORT_FILE = "benchmark_report.md"
def REPORT_SCRIPT= "pipelines/report.sh"
def START_TIME_MS = System.currentTimeMillis()
pipeline {
    agent {
        label "built-in"
    }
    options {
        // Capture the start time precisely for overall duration calculation
        timeout(time: 60, unit: 'MINUTES')
    }
    environment {
        // Environment variables to pass results to the Bash reporter
        TEST_FILE = "${WORKSPACE}/io_test.benchmark"
        FILE_SIZE = '1G' // Test with 1 Gigabyte
    }
    stages {
        stage('Initialize & Record Start Time') {
            steps {
                script {
                    echo "Starting benchmark run at: ${new Date(START_TIME_MS)}"
                }
                // Make the script executable
                sh "chmod  +x ${REPORT_SCRIPT}"
            }
        }
        stage('File I/O Benchmark to JENKINS_HOME') {
            steps {
                sh label: 'Sequential Write Test', script: """
                    echo "--- Running Sequential Write Test (${FILE_SIZE} file) ---"
                    # Write test using dd (writing to the mounted workspace volume)
                    # Use oflag=dsync ensures data is physically written, testing IOPS/throughput accurately.
                    dd if=/dev/zero of=\${TEST_FILE} bs=1M count=1024 oflag=dsync status=progress 2>&1 | tee write_output.txt

                    # Extract speed (requires the speed output from dd, typically the last line)
                    WRITE_SPEED=\$(cat write_output.txt | grep -Eo '[0-9.]+ [MGK]B/s' | tail -1)
                    echo "Extracted Write Speed: \${WRITE_SPEED}"

                    echo "\${WRITE_SPEED}" > write_speed.txt
                """
                sh label: 'Sequential Read Test',script:  """
                    echo "--- Running Sequential Read Test (${FILE_SIZE} file) ---"
                    # Read test from the written file
                    # iflag=direct bypasses filesystem cache, testing raw disk speed.
                    dd if=${TEST_FILE} of=/dev/null bs=1M iflag=direct status=progress 2>&1 | tee read_output.txt

                    READ_SPEED=\$(cat read_output.txt | grep -Eo '[0-9.]+ [MGK]B/s' | tail -1)
                    echo "Extracted Read Speed: \${READ_SPEED}"

                    echo "\${READ_SPEED}" > read_speed.txt

                    # Clean up
                    rm -f ${TEST_FILE}
                """
            }
        }
        stage('CPU & Memory Load Simulation') {
            steps {
                sh label: 'Run CPU Load Test',script:  """
                    echo "--- Running CPU Load Test (bc calculation) ---"
                    # Run the Bash script's CPU test function and capture the raw time output
                    # The script prints the time as the last line, making it easy to capture with backticks.
                    CPU_TIME=\$(${REPORT_SCRIPT} cpu_test)

                    # Extract only the numeric part (which the script echoes) and ensure it's exported
                    CPU_TIME_CLEAN=\$(echo "\${CPU_TIME}" | tail -1)

                    echo "Captured CPU Runtime: \${CPU_TIME_CLEAN}s"
                    echo "\${CPU_TIME_CLEAN}" > cpu_runtime.txt
                """
                sh label: 'Run Memory Bandwidth Test',script:  """
                    echo "--- Running Memory Bandwidth Test (dd) ---"
                    # Run the Bash script's memory test function and capture the output
                    MEM_SPEED=\$(${REPORT_SCRIPT} memory_test)

                    echo "Captured Memory Bandwidth: \${MEM_SPEED}"
                    echo "\${MEM_SPEED}" > mem_bandwidth.txt
                """
            }
        }
        stage('Generate Final Report') {
            steps {
                script {
                    def END_TIME_MS = System.currentTimeMillis()
                    def TOTAL_DURATION = (END_TIME_MS - START_TIME_MS) / 1000.0

                    // Set the final duration variable for the reporter script
                    env.TOTAL_TIME_SECONDS = TOTAL_DURATION.toString()
                    echo "Total elapsed time: ${TOTAL_DURATION} seconds"

                    env.IO_WRITE_SPEED = readFile('write_speed.txt').trim()
                    env.IO_READ_SPEED = readFile('read_speed.txt').trim()
                    env.CPU_RUNTIME = readFile('cpu_runtime.txt').trim()
                    env.MEM_BANDWIDTH = readFile('mem_bandwidth.txt').trim()
                }

                // Execute the Bash reporter script to compile the results
                sh "IO_WRITE_SPEED='${env.IO_WRITE_SPEED}' IO_READ_SPEED='${env.IO_READ_SPEED}' CPU_RUNTIME='${env.CPU_RUNTIME}' MEM_BANDWIDTH='${env.MEM_BANDWIDTH}' TOTAL_TIME_SECONDS='${env.TOTAL_TIME_SECONDS}' ${REPORT_SCRIPT} generate_report" // Referenced the renamed script
            }
        }
        stage('Archive Report') {
            steps {
                // Archive the generated report so it's easily viewable on the build page
                archiveArtifacts artifacts: REPORT_FILE, fingerprint: true
                archiveArtifacts artifacts: "*.txt", fingerprint: true
            }
        }
    }
}
