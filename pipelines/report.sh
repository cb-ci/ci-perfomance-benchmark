#!/bin/bash

# Define constants
REPORT_FILE="benchmark_report.md"
CPU_LOAD_ITERATIONS=15000 # Number of iterations for the 'bc' calculation stress

# --- Utility Functions ---

# Function to convert seconds (potentially with decimals) to HH:MM:SS.s format
# Input: total seconds (e.g., 3660.5)
format_time() {
    local total_seconds=$1
    local seconds_part=$(echo "$total_seconds % 60" | bc)
    local minutes_total=$(echo "$total_seconds / 60" | bc)
    local hours_total=$(echo "$minutes_total / 60" | bc)

    local hours=$(printf "%.0f" "$hours_total")
    local minutes=$(printf "%.0f" "$(echo "$minutes_total % 60" | bc)")

    printf "%02d:%02d:%04.1f" "$hours" "$minutes" "$seconds_part"
}

# Function to run the CPU stress test and echo the runtime
cpu_test() {
    # We use 'openssl speed' to benchmark the CPU's performance with cryptographic functions.
    # This provides a standardized and intensive workload.

    echo "Starting CPU stress test (openssl speed)..."
    local start_time=$(date +%s.%N)

    # Run a short but intensive benchmark on a common algorithm
    openssl speed -elapsed -evp aes-256-cbc > /dev/null 2>&1

    local end_time=$(date +%s.%N)
    # Calculate duration
    local runtime=$(echo "$end_time - $start_time" | bc)

    echo "Extracted CPU Runtime: ${runtime}s"
    echo "$runtime" # Echo the raw runtime for Jenkins to capture
}

# Function to run a memory bandwidth test
memory_test() {
    echo "Starting Memory bandwidth test (dd)..."
    # Use dd to move a 1GB block of data and measure the speed.
    # This tests memory bandwidth by reading from a null source and writing to a null sink.
    local mem_speed=$(dd if=/dev/zero of=/dev/null bs=1M count=1024 2>&1 | grep -o '[0-9.]* [a-zA-Z/]*s$')

    echo "Extracted Memory Bandwidth: ${mem_speed}"
    echo "${mem_speed}" # Echo the result for Jenkins to capture
}

# Function to generate the final Markdown report
generate_report() {
    # Read environment variables
    local TOTAL_TIME_FORMATTED=$(format_time "$TOTAL_TIME_SECONDS")
    local DATE_TIME=$(date '+%Y-%m-%d %H:%M:%S')
    cat << EOF > "$REPORT_FILE"
# Controller Performance Benchmark Report

**Generated at:** $DATE_TIME
**Pipeline Run Duration:** $TOTAL_TIME_FORMATTED

---

## 1. File I/O Benchmark (${FILE_SIZE} Sequential Test)
*(Testing read/write speed against the volume hosting the workspace)*

| Metric | Result |
| :--- | :--- |
| Sequential Write Speed | **${IO_WRITE_SPEED}** |
| Sequential Read Speed | **${IO_READ_SPEED}** |

## 2. CPU Stress Test
*(Testing raw computation time via 'openssl speed')*

| Metric | Result |
| :--- | :--- |
| CPU Load Runtime | **${CPU_RUNTIME}** seconds |
| Load Intensity | openssl speed -evp aes-256-cbc |

## 3. Memory Bandwidth Test
*(Testing memory throughput with 'dd')*

| Metric | Result |
| :--- | :--- |
| Memory Bandwidth | **${MEM_BANDWIDTH}** |

## 4. Overall Controller Performance Summary
The overall pipeline duration (which includes checkout, agent spin-up, and all steps) is the best indicator of overall controller responsiveness under load.

**Total Execution Time:** **$TOTAL_TIME_FORMATTED**
EOF

    echo "Successfully generated $REPORT_FILE"
}

# --- Main Execution Logic ---
if [ "$1" == "cpu_test" ]; then
    # If run with 'cpu_test' argument, only run the test and output the time
    cpu_test
elif [ "$1" == "memory_test" ]; then
    # If run with 'memory_test' argument, only run the test and output the result
    memory_test
elif [ "$1" == "generate_report" ]; then
    # If run with 'generate_report', generate the final report
    generate_report
else
    echo "Usage: $0 {cpu_test|memory_test|generate_report}"
    exit 1
fi
