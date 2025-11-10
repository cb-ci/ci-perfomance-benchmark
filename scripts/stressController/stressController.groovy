// Stress Test Scheduler for Jenkins Script Console

// --- Configuration ---
// Define your stress test jobs here. Each job is a map with the following keys:
// - name: A descriptive name for the job.
// - cpuThreads: The number of threads to dedicate for CPU stress.
// - memoryMB: The amount of memory in megabytes to allocate.
// - durationSeconds: The duration of the stress test in seconds.

def stressJobs = [
    [name: "Light Load",    cpuThreads: 2, memoryMB: 128, durationSeconds: 15],
    [name: "Moderate Load", cpuThreads: 4, memoryMB: 256, durationSeconds: 30],
    [name: "Heavy Load",      cpuThreads: 8, memoryMB: 512, durationSeconds: 60]
]

// --- Main Execution ---

println "Starting stress test scheduler..."

stressJobs.eachWithIndex { job, index ->
    println "---"
    println "Starting Job #${index + 1}: '${job.name}'"
    println "  - CPU Threads: ${job.cpuThreads}"
    println "  - Memory (MB): ${job.memoryMB}"
    println "  - Duration (s): ${job.durationSeconds}"

    // 1. Memory Allocation
    def allocatedMemory = []
    def bytesPerMB = 1024 * 1024
    try {
        (0..<job.memoryMB).each {
            allocatedMemory << new byte[bytesPerMB]
        }
        println "  Successfully allocated ${job.memoryMB}MB of memory."
    } catch (OutOfMemoryError e) {
        println "  ERROR: Failed to allocate ${job.memoryMB}MB of memory. OutOfMemoryError."
        // Stop this job and move to the next one
        return
    }

    // 2. CPU Stress
    def threads = (1..job.cpuThreads).collect {
        Thread.start {
            def endTime = System.currentTimeMillis() + (job.durationSeconds * 1000)
            while (System.currentTimeMillis() < endTime) {
                // Perform a CPU-intensive operation
                Math.sqrt(new Random().nextDouble())
            }
        }
    }
    println "  Started ${job.cpuThreads} CPU stress threads."

    // 3. Wait for job completion
    threads*.join()
    println "  Job '${job.name}' completed."

    // 4. Release memory (optional, as GC will handle it, but good for clarity)
    allocatedMemory = null
    System.gc()
}

println "---"
println "All stress test jobs completed."