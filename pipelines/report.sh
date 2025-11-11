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
run_cpu_test() {
    # We use a simple loop with 'bc' to perform intensive calculations,
    # as recursive functions like Fibonacci are difficult and inefficient in pure Bash.

    echo "Starting CPU stress calculation (Iterations: $CPU_LOAD_ITERATIONS)..."
    local start_time=$(date +%s.%N)

    for i in $(seq 1 $CPU_LOAD_ITERATIONS); do
        # Perform a calculation using arbitrary precision math via bc
        echo "scale=10; \sqrt($i) * (3.14159 / (1 + $i))" | bc -l > /dev/null
    done

    local end_time=$(date +%s.%N)
    # Calculate duration
    local runtime=$(echo "$end_time - $start_time" | bc)

    echo "Extracted CPU Runtime: ${runtime}s"
    echo "$runtime" # Echo the raw runtime for Jenkins to capture
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

## 1. File I/O Benchmark (\${FILE_SIZE} Sequential Test)
*(Testing read/write speed against the volume hosting the workspace)*

| Metric | Result |
| :--- | :--- |
| Sequential Write Speed | **${IO_WRITE_SPEED}** |
| Sequential Read Speed | **${IO_READ_SPEED}** |

## 2. CPU Stress Test
*(Testing raw computation time via intensive 'bc' calculations)*

| Metric | Result |
| :--- | :--- |
| CPU Load Runtime | **${CPU_RUNTIME}** seconds |
| Load Intensity | $CPU_LOAD_ITERATIONS iterations (bc) |

## 3. Overall Controller Performance Summary
The overall pipeline duration (which includes checkout, agent spin-up, and all steps) is the best indicator of overall controller responsiveness under load.

**Total Execution Time:** **$TOTAL_TIME_FORMATTED**
EOF

    echo "Successfully generated $REPORT_FILE"
}

# --- Main Execution Logic ---
if [ "$1" == "cpu_test" ]; then
    # If run with 'cpu_test' argument, only run the test and output the time
    run_cpu_test
elif [ "$1" == "generate_report" ]; then
    # If run with 'generate_report', generate the final report
    generate_report
else
    echo "Usage: $0 {cpu_test|generate_report}"
    exit 1
fi
