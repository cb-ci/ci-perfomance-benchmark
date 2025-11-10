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

// Run the test
testDiskPerformance()