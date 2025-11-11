# Performance Testing Guide for Jenkins Controllers

**Table of Contents**

* [Test Plan Summary](#test-plan-summary)
* [About Controller Performance](#about-controller-performance)
* [Goal](#goal)
* [Scope](#scope)
* [Strategy](#strategy)
* [Key Metrics](#key-metrics)
* [Entry and Exit Criteria](#entry-and-exit-criteria)
* [Requirements/Tooling](#requirementstooling)
* [Tooling Stack](#tooling-stack)
* [Roles and Responsibilities](#roles-and-responsibilities)
* [Key Performance Indicators (SLOs)](#key-performance-indicators-slos)
* [Risks and Mitigations](#risks-and-mitigations)
* [Schedule/Timeline](#scheduletimeline)
* [Workload and Testing Strategy](#workload-and-testing-strategy)
  * [Pipelines and Workload Models](#pipelines-and-workload-models)
  * [Testing per-Pipeline](#testing-per-pipeline)
* [Practical Snippets](#practical-snippets)
  * [Locust for Load Testing](#locust-for-load-testing)
  * [PR Storm with gh CLI](#pr-storm-with-gh-cli)
  * [Trigger Builds via REST API](#trigger-builds-via-rest-api)
  * [Simple Pipeline End-to-End duration](#simple-pipeline-end-to-end-duration)
* [Common Bottlenecks and Fixes](#common-bottlenecks-and-fixes)

## Test Plan Summary

This document outlines a comprehensive strategy for performance-testing Jenkins controllers, providing a step-by-step guide to measuring and optimizing platform and build metrics.

## About Controller Performance

On a healthy setup, the **controller** is primarily responsible for:

* **Pipeline Engine:** Executing Groovy code in `Jenkinsfile` and shared libraries.
* **Scheduling:** Managing the build queue and executor metadata.
* **Logging and UI:** Handling console output and user interface elements.
* **Plugin Overhead:** Managing plugins like GitHub Branch Source and webhooks.

Since Jenkins controllers are I/O bound, the performance of the storage used for `JENKINS_HOME` is critical. 
The complexity of Jenkinsfiles, the efficiency of shared libraries, and the volume performance, all contribute to the controller's footprint.

## Goal

Compare the performance of two CI Controller setups:

* **Source Controller:** Running on traditional infrastructure (EC2 VM with EBS volume).
* **Target Controller:** Running on modern infrastructure (EKS with EFS volume).

Both controllers have identical configurations, including plugins, agent setups, and reference pipelines.

## Scope

**In Scope:**

*   Performance comparison between the Source Controller (EC2/EBS) and Target Controller (EKS/EFS).
*   Controller-level metrics (CPU, memory, GC, etc.).
*   Pipeline-level metrics (throughput, queue time, etc.).
*   File I/O performance on `JENKINS_HOME`.

**Out of Scope:**

*   Performance of Jenkins agents under load.
*   Network latency between agents and the controller.
*   Performance of external services integrated with Jenkins (e.g., artifact repositories, Git providers).


## Strategy

The primary approach is to measure the CPU and memory consumption **delta against a baseline** for various workloads. This involves:

1. **Establishing a Baseline:** 
   * Measure CPU and memory usage when the controller is idle.
   * Measure file IO (latency, IOOPS and throughput) 
2. **Running Test Workloads:** Execute representative Pipelines at different scales (e.g., 1, 10, 50 concurrent runs).
3. **Analyzing the Delta:** Subtract the baseline measurements from the workload measurements to determine the approximate resource cost per execution.

## Key Metrics

The following key performance indicators (KPIs) will be tracked:

* **JVM Heap, Non-Heap and Old Gen Usage**
* **Process CPU Load**
* **Garbage Collection (GC) Pause Time**
* **Throughput** (builds/min)
* **Queue Time**
* **Job Start Latency**
* **Agent Provisioning Time**
* **File IO/volume** IOOPS,throughput
* **Pipeline End-to-End exection time**

## Entry-and-Exit-Criteria

* Entry Criteria
  *   Both Source and Target controllers are fully configured and operational.
  *   The observability stack (Prometheus, Grafana, OpenTelemetry) is in place and collecting data.
  *   Test pipelines and load generation scripts are ready.

* Exit Criteria
  *   All test scenarios have been executed at least three times on both controller setups.
  *   Key metrics have been collected and documented.
  *   A final analysis report comparing the performance of the two setups is complete.

## Tooling Stack

* **Load Generation:**
  * **Locust:** For hitting Jenkins job REST API to trigger builds. [Locust README](scripts/loadtestsLocust/README.md)
  * **gh CLI:** For creating commits and pull requests at scale.
* **Orchestration:**
  * Target Controller: A dedicated **"Load Orchestrator" Jenkins controller** to manage test execution.
  * Source Controller: A dedicated **"Load Orchestrator" Jenkins controller** to manage test execution. If a Production controller be carefully with the workload simulations
* **Observability:**
  * **Metrics:** Prometheus + Grafana for Jenkins, JVM, VM, and Kubernetes metrics.
  * **Traces:** OpenTelemetry (Jenkins OTel plugin) for distributed tracing.
  * **Logs:** Console log/Pipeline Explorer,Splunk, ELK, or other log aggregation solutions.

## Roles and Responsibilities

| Role               | Responsibilities                                        |
|--------------------| ------------------------------------------------------- |
| **Test Lead**      | Overall test plan, execution, and analysis.             |
| **Infra Engineer** | Setup and maintenance of controller environments.       |
| **Developer**      | Development and maintenance of test pipelines.          |

## Key Performance Indicators (SLOs)

* **Throughput:** ≥ X builds/min per controller.
* **Queue Time (P95):** < 15s (steady) / < 60s (burst).
* **Job Start Latency (P95):** < 20s (steady) / < 90s (burst).
* **Controller Headroom:** CPU < 70%, Heap < 70%, GC Pause (P99) < 200ms.
* **Pipeline End-to-End execution time** See traces or Pipeline Explorer

---

## Risks and Mitigations

| Risk                                     | Mitigation                                                              |
| ---------------------------------------- | ----------------------------------------------------------------------- |
| **Inaccurate test results**              | Run baseline tests and repeat each test multiple times for consistency. |
| **API rate limiting from Git provider**  | Check rate limits and throttle tests, or use a dedicated test org.      |
| **Environment instability**              | Monitor environments closely and have rollback plans in place.          |

## Schedule/Timeline

| Phase                 | Estimated Duration |
| --------------------- |--------------------|
| **Environment Setup** | less than 1 day    |
| **Baseline Testing**  | 1 day              |
| **Workload Execution**| less than 2 days   |
| **Analysis & Report** | 1 day              |

---

## Workload and Testing Strategy

### Pipelines and Workload Models

To simulate real-world usage, we will use a mix of test scenarios:

* **Steady-State Build Mix:** A combination of short, medium, and heavy pipelines.
* **PR/Build Storm:** A high volume of webhooks in a short period.
* **Multibranch Indexing Wave:** Concurrent indexing of a large number of repositories.
* **Agent Churn:** (Optional): Rapid creation and tear down of ephemeral agents.

Suggestions for test/benchmark Pipelines

* A simple reference MB Pipeline that
  * clones code (small to medium size repo)
  * writes an artifact to JENKINS_HOME (step: archiveArtifacts, to measure time by writing to the volume EBS vs EFS)
  * See this pipelines for starting point: [pipelines](pipelines) (Need to be developed/adjusted further)
* A reference/representative (real production Pipeline) 
  * fork the repository (single or few branches only)
  * adjust the Pipeline fork code/branch so it doesn't damage anything in production if it gets executed
* test/compare on both Pipelines
  * MI Indexing and Scanning
  * Scaling the number of builds/commits (1,5,10)
  * Scaling the number of PRs (1,5,10)
  * Build time/performance

### Testing per-Pipeline

To isolate the performance impact of specific jobs:

1. **Run in Isolation:** Execute one pipeline at a time to measure its individual footprint.
2. **Scale Testing:** Run multiple copies of the same pipeline (e.g., 1, 5, 10) to understand how the controller scales and to determine its capacity for concurrent runs.

---


## Practical Snippets

### Managing Repos, Branches and PRs

See [README.md](scripts/gh-scripts/README.md)[gh-scripts](scripts/gh-scripts)

### File IO

See

* [README.md](scripts/fileIO/README.md)
* [runFIOPod.sh](scripts/fileIO/runFIOPod.sh)
* [filePerfIOMetrics.groovy](scripts/fileIO/filePerfIOMetrics.groovy)

### Sample basic Pipelines

See 
* [Jenkinsfile-benchmarks.groovy](pipelines/Jenkinsfile-benchmarks.groovy)
* [pipelines](pipelines). 

Note: These Pipelines can be developed further/adjusted 

### Locust for Load Testing

See the [Locust README](scripts/loadtestsLocust/README.md) for details on setting up and running Locust tests.

### PR Storm with `gh` CLI

```bash
org=perf
for r in $(seq 1 1000); do
  repo=repo-$r
  gh repo create $org/$repo --private -y
  git clone "https://github.com/$org/$repo"
  pushd $repo
  echo "initial" > README.md
  git add . && git commit -m "init" && git push -u origin main
  for i in $(seq 1 10); do
    git checkout -b feat-$i
    echo $RANDOM >> file.txt
    git add . && git commit -m "feat $i"
    git push -u origin HEAD
    gh pr create --fill --title "feat $i" --body "load test"
  done
  popd
done
```

### Trigger Builds via REST API

See also [triggerSimplePipeline.sh](scripts/triggerSimplePipeline.sh)

```bash
JENKINS=https://jenkins.example
USER=svc
TOKEN=xxx
JOB='perf/LoadJob'

for i in {1..10}; do
  curl -s -u "$USER:$TOKEN" \
    -X POST "$JENKINS/job/$JOB/buildWithParameters?PROFILE=medium&cause=perf"
done
```

### Verify Simple Pipeline End-to-End duration

Options

* A:Setup Open telemtry plugin and anayse/compare the traces
* B: Use the Pipeline Explorer or Console log
* C: Use the UI build summary page
* D: Simple Pipeline End-to-End duration time can also be retrieved by the build_url api json endpoint: 
`curl -u $TOKEN "${BUILD_URL}/api/json" | jq`
result example:
```
{
  "_class": "org.jenkinsci.plugins.workflow.job.WorkflowRun",
  "actions": [
   ...
    {
      "_class": "jenkins.metrics.impl.TimeInQueueAction",
      "blockedDurationMillis": 0,
      "blockedTimeMillis": 0,
      "buildableDurationMillis": 0,
      "buildableTimeMillis": 4797,
      "buildingDurationMillis": 7730,
      "executingTimeMillis": 1814,
      "executorUtilization": 0.23,
      "subTaskCount": 1,
      "waitingDurationMillis": 0,
      "waitingTimeMillis": 0
    },
...
  "duration": 7730,
  "estimatedDuration": 9641,
  "id": "2",
  "keepLog": false,
  "number": 2,
  "queueId": 504,
  "result": "SUCCESS",
  "timestamp": 1759332317591,
  "url": "https://dev.sda.acaternberg.flow-training.beescloud.com/casc-pipeline-templates/job/HW/2/",
  ...
}
```

Which means:

```
4.8 sec waiting;
7.7 sec build duration;
7.7 sec total from scheduled to completion
```

---

## Common Bottlenecks and Fixes

* **Webhook Bottlenecks:** Shard by organization/repository, scale ingress, and enable keep-alive.
* **SCM Scans Slow:** Enable **reference repos** and **sparse checkout**, and reduce branch discovery rules.
* **GC Pauses:** Adjust G1GC settings, right-size the heap, and avoid log amplification.
* **File IO** IOPS, throughput (EBS/EFS,Node disk type) [filePerfIOMetrics.groovy](scripts/fileIO/filePerfIOMetrics.groovy)
