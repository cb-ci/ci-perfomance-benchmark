def REPORT_FILE = "benchmark_report.md"
def REPORT_SCRIPT= "pipelines/report.sh"
def START_TIME_MS = System.currentTimeMillis()

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

def testDiskPerformance() {
    // --- Configuration ---
    final def testFile = new File("/tmp/jenkins_disk_perf_test.dat")
    final def chunkSizes = [1024, 4096, 16384] // 1KB, 4KB, 16KB
    final def numOperations = 10000
    final def fileSize = 100 * 1024 * 1024 // 100MB

    println "=== Jenkins Disk Performance Test - Random Writes ==="
    println "Test file: ${testFile.absolutePath}"
    println "Operations per test: ${numOperations}"
    println "File size: ${fileSize / (1024*1024)}MB"
    println ""

    try {
        // --- Pre-allocate File ---
        // This ensures blocks are physically on disk, avoiding on-the-fly allocation
        // during the test, which would skew latency measurements.
        println "Pre-allocating ${fileSize / (1024 * 1024)}MB test file..."
        final long preallocStart = System.nanoTime()
        testFile.withOutputStream { out ->
            // Use a 1MB buffer to write data in chunks for efficiency
            final byte[] buffer = new byte[1024 * 1024]
            (fileSize / buffer.length).times { out.write(buffer) }
        }
        final long preallocDuration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - preallocStart)
        println "Pre-allocation took ${preallocDuration} ms"
        println ""

        chunkSizes.each { chunkSize ->
            println "--- Testing ${chunkSize} byte chunks ---"

            final def latencies = []
            final def random = new Random()
            final long maxPosition = fileSize - chunkSize

            // --- Prepare Data Buffer ---
            // Use a direct ByteBuffer for potentially better I/O performance.
            // The OS may be able to perform I/O directly on this buffer,
            // avoiding a copy from the JVM heap.
            final ByteBuffer dataBuffer = ByteBuffer.allocateDirect(chunkSize)
            final byte[] randomBytes = new byte[chunkSize]
            random.nextBytes(randomBytes)
            dataBuffer.put(randomBytes)

            final long startTime = System.currentTimeMillis()

            // --- Run Performance Test ---
            // Open FileChannel with WRITE and SYNC options.
            // SYNC ensures that every write (content and metadata) is flushed
            // to the storage device, which is crucial for accurate latency measurement.
            // This makes an explicit channel.force() call inside the loop redundant.
            FileChannel.open(
                    testFile.toPath(),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.SYNC
            ).withCloseable { channel ->
                numOperations.times {
                    final long position = (long)(random.nextInt((int)(maxPosition / chunkSize)) * chunkSize)

                    final long opStart = System.nanoTime()
                    dataBuffer.rewind() // Reset buffer position for each write
                    channel.write(dataBuffer, position)
                    final long opEnd = System.nanoTime()

                    latencies << (opEnd - opStart) / 1_000_000.0 // Convert ns to ms
                }
            } // FileChannel is automatically closed here by withCloseable

            final double totalTime = (System.currentTimeMillis() - startTime) / 1000.0

            // --- Calculate and Print Metrics ---
            if (latencies.isEmpty()) {
                println "  No latency data collected."
                return // continue to next chunk size
            }

            final double avgLatency = latencies.sum() / latencies.size()
            final double iops = totalTime > 0 ? numOperations / totalTime : 0
            final double throughputMBps = totalTime > 0 ? (numOperations * chunkSize) / (totalTime * 1024 * 1024) : 0

            // Calculate p95 latency
            final def sortedLatencies = latencies.sort() // sort() returns a new sorted list
            final int p95Index = (int)(sortedLatencies.size() * 0.95)
            final double p95Latency = sortedLatencies[p95Index]

            println "  IOPS:              ${String.format('%.2f', iops)} ops/sec"
            println "  Throughput:        ${String.format('%.2f', throughputMBps)} MB/s"
            println "  Avg Latency:       ${String.format('%.2f', avgLatency)} ms"
            println "  P95 Latency:       ${String.format('%.2f', p95Latency)} ms"
            println "  Total Time:        ${String.format('%.2f', totalTime)} seconds"
            println ""
        }

    } finally {
        // --- Cleanup ---
        if (testFile.exists()) {
            testFile.delete()
            println "Test file cleaned up"
        }
    }
}



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
                // Run the test
                testDiskPerformance()
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

    }
    post {
        success {
            sh """                
                    # Record end time
                    export end_time=\$(date +%s)
                    
                    # Compute elapsed time in seconds
                    elapsed=\$(( end_time - ${START_TIME_MS} ))
                    
                    # Optional: format elapsed time as H:M:S
                    hours=\$(( elapsed / 3600 ))
                    minutes=\$(( (elapsed % 3600) / 60 ))
                    seconds=\$(( elapsed % 60 ))
                    
                    echo ">>> Script finished at: \$(date)"
                    echo ">>> Total execution time: \${hours}h \${minutes}m \${seconds}s (\${elapsed}s total)"
                    
                 """
            script {
                //def END_TIME_MS = System.currentTimeMillis()
                //def TOTAL_DURATION = (END_TIME_MS - START_TIME_MS) / 1000.0
                def TOTAL_DURATION = ($end_time - START_TIME_MS) / 1000.0

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
            // Archive the generated report so it's easily viewable on the build page
            archiveArtifacts artifacts: REPORT_FILE, fingerprint: true
            archiveArtifacts artifacts: "*.txt", fingerprint: true

        }
    }
}
