# Performance Testing Guide for Jenkins Controllers

**Table of Contents**

* [Test Plan Summary](#test-plan-summary)
  * [Goal](#goal)
  * [Strategy](#strategy)
  * [Key Metrics](#key-metrics)
* [Understanding Controller Performance](#understanding-controller-performance)
* [Workload and Testing Strategy](#workload-and-testing-strategy)
  * [Workload Models](#workload-models)
  * [Testing per-Pipeline](#testing-per-pipeline)
* [Tooling and Observability](#tooling-and-observability)
  * [Tooling Stack](#tooling-stack)
  * [Key Performance Indicators (SLOs)](#key-performance-indicators-slos)
* [Practical Snippets](#practical-snippets)
  * [Locust for Load Testing](#locust-for-load-testing)
  * [PR Storm with gh CLI](#pr-storm-with-gh-cli)
  * [Trigger Builds via REST API](#trigger-builds-via-rest-api)
* [Common Bottlenecks and Fixes](#common-bottlenecks-and-fixes)

This document outlines a comprehensive strategy for performance-testing Jenkins controllers, providing a step-by-step guide to measuring and optimizing platform and build metrics.

## Test Plan Summary


### About Controller Performance

On a healthy setup, the **controller** is primarily responsible for:

* **Pipeline Engine:** Executing Groovy code in `Jenkinsfile` and shared libraries.
* **Scheduling:** Managing the build queue and executor metadata.
* **Logging and UI:** Handling console output and user interface elements.
* **Plugin Overhead:** Managing plugins like GitHub Branch Source and webhooks.

Since Jenkins controllers are I/O bound, the performance of the storage used for `JENKINS_HOME` is critical. 
The complexity of Jenkinsfiles, the efficiency of shared libraries, and the volume performance, all contribute to the controller's footprint.

### Goal

Compare the performance of two CI Controller setups:

* **Source Controller:** Running on traditional infrastructure (EC2 VM with EBS volume).
* **Target Controller:** Running on modern infrastructure (EKS with EFS volume).

Both controllers have identical configurations, including plugins, agent setups, and reference pipelines.

### Strategy

The primary approach is to measure the CPU and memory consumption **delta against a baseline** for various workloads. This involves:

1. **Establishing a Baseline:** 
   * Measure CPU and memory usage when the controller is idle.
   * Measure file IO (latency, IOOPS and throughput) 
2. **Running Test Workloads:** Execute representative Pipelines at different scales (e.g., 1, 10, 50 concurrent runs).
3. **Analyzing the Delta:** Subtract the baseline measurements from the workload measurements to determine the approximate resource cost per execution.

### Key Metrics

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

---

## Workload and Testing Strategy

### Workload Models

To simulate real-world usage, we will use a mix of test scenarios:

1. **Steady-State Build Mix:** A combination of short, medium, and heavy pipelines.
2. **PR/Build Storm:** A high volume of webhooks in a short period.
3. **Multibranch Indexing Wave:** Concurrent indexing of a large number of repositories.
4. **Agent Churn:** (Optional): Rapid creation and tear down of ephemeral agents.

### Testing per-Pipeline

To isolate the performance impact of specific jobs:

1. **Run in Isolation:** Execute one pipeline at a time to measure its individual footprint.
2. **Scale Testing:** Run multiple copies of the same pipeline (e.g., 1, 5, 10) to understand how the controller scales and to determine its capacity for concurrent runs.

---

## Requirements/Tooling

### Tooling Stack

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

### Key Performance Indicators (SLOs)

* **Throughput:** ≥ X builds/min per controller.
* **Queue Time (P95):** < 15s (steady) / < 60s (burst).
* **Job Start Latency (P95):** < 20s (steady) / < 90s (burst).
* **Controller Headroom:** CPU < 70%, Heap < 70%, GC Pause (P99) < 200ms.
* **Pipeline End-to-End execution time** See traces or Pipeline Explorer

---

## Practical Snippets

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
CRUMB=$(curl -s -u "$USER:$TOKEN" "$JENKINS/crumbIssuer/api/json" | jq -r .crumb)

for i in {1..10}; do
  curl -s -u "$USER:$TOKEN" -H "Jenkins-Crumb:$CRUMB" \
    -X POST "$JENKINS/job/$JOB/buildWithParameters?PROFILE=medium&cause=perf"
done
```

### Simple Pipeline End-to-End duration


Simple Pipeline End-to-End duration time can be retrieved by 

* The UI `open $BUILD_URL` 
* or by the build_url api json endpoint:  `curl -u $TOKEN "${BUILD_URL}/api/json" | jq`

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
