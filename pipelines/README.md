# Pipeline Onboarding Guide

## Overview
This directory contains a set of Jenkins pipelines and helper scripts designed to:
- Demonstrate parent/child pipeline interactions
- Simulate and benchmark controller and agent load
- Provision multibranch benchmark jobs via Jenkins Configuration-as-Code

They can be used for some reference test to get the "baseline" of a Controller
The Pipelines focus on:

* File IO (Sequential only, for random see [README.md](../scripts/fileIO/README.md))
* CPU
* Memory

## Prerequisites
- Jenkins instance (CloudBees or open-source) with:
    - Pipeline (Declarative) plugin
    - Kubernetes plugin (for `Jenkinsfile-StressNG.groovy`)
    - Credentials: GitHub App or token-based credentials
    - `triggerRemoteJob` plugin (for `Jenkinsfile-root.groovy`)
- Bash shell utilities (`bash`, `dd`, `curl`, `awk`, `openssl`)
- Access to a Kubernetes cluster (for Pod agents)

## Repository Structure
 ```text
 .
 ├── Jenkinsfile-root.groovy          # Parent pipeline example with remote trigger
 ├── Jenkinsfile-child.groovy         # Downstream pipeline example (parameterized)
 ├── Jenkinsfile-mockStepController.groovy # Mock load step controller demo
 ├── Jenkinsfile-benchmarks.groovy    # File I/O, CPU & memory benchmarking
 ├── Jenkinsfile-StressNG.groovy      # High-load stress-ng performance pipeline
 ├── report.sh                        # Bash reporter script for benchmarks
 ├── set-env.sh                       # Environment configuration script
 ├── createMBBenchmarkJob.sh          # Script to provision MB-Benchmark job via C-as-Code
 ├── items-MBBenchmark.yaml           # YAML config for multibranch benchmark job
 └── README-Pipeline-onboarding.md    # (This file) pipeline onboarding guide
 ```

## Pipeline Files and Their Purpose

### Jenkinsfile-root.groovy
- Demonstrates a parent Declarative pipeline (agent none) that:
    - Prints a greeting (`echo 'Hello World'`)
    - Uses `triggerRemoteJob` to invoke a child pipeline (`test-triggers/child`)
    - Shows parameter passing between pipelines
- Requires two script approvals:
  ```
  jenkins.model.Jenkins.get()
  jenkins.model.Jenkins.getLegacyInstanceId()
  ```

### Jenkinsfile-child.groovy
- A simple downstream pipeline (agent none) with:
    - A string parameter `paramKey1`
    - A stage that echoes the passed parameter
- Used to illustrate parent → child parameter propagation

### Jenkinsfile-mockStepController.groovy
- Runs on any agent (`agent any`)
- Invokes a custom step `mockLoad 10` inside a `script{}` block
- Useful for testing step-controller logic or plugin development

### Jenkinsfile-benchmarks.groovy
- Declarative pipeline that benchmarks:
    1. File I/O (sequential write/read using `dd`)
    2. CPU performance (via `openssl speed`)
    3. Memory bandwidth (via `dd` to `/dev/null`)
- Uses `report.sh` to generate a Markdown report (`benchmark_report.md`)
- Archives artifacts: report and raw speed.txt files
- Timeout set to 60 minutes; runs on a `built-in` (controller) label

### Jenkinsfile-StressNG.groovy
- High-load performance pipeline leveraging Kubernetes Pod agents
- Defines a common pod spec (`yamlPod`) with `git` and `stress-ng` containers
- Executes parallel stages including:
    - Cloning a large repository (e.g., Linux kernel)
    - Running `stress-ng` in agent pods
    - Simulating controller-side CPU/memory stress in Groovy
    - Disk I/O test with `dd` and stash/unstash
    - Workspace cleanup
- Designed for testing Jenkins controller and agent resilience under load

## Helper Scripts and Configurations

- `report.sh`: Bash functions (`cpu_test`, `memory_test`, `generate_report`) to:
    - Measure CPU and memory performance
    - Format results into a `benchmark_report.md`
- `set-env.sh`: Export environment variables for:
    - `JENKINS_HOST`, `JENKINS_API_TOKEN`, and `JENKINS_JOB_PATH`
- `createMBBenchmarkJob.sh`: Uses `items-MBBenchmark.yaml` template with:
    - `envsubst` to inject variables
    - `curl` to POST job definition to the Jenkins C-as-Code endpoint
- `items-MBBenchmark.yaml`: Configuration for a multibranch pipeline job
    - Points to `pipelines/Jenkinsfile-benchmarks.groovy`
    - Includes branch and PR discovery traits for GitHub

## Getting Started
1. Clone this repository on your Jenkins server or local machine.
2. Update `set-env.sh` with your Jenkins URL, API token, and job path.
3. Source the environment: `source set-env.sh`
4. Provision the multibranch benchmark job:
   ```bash
   ./createMBBenchmarkJob.sh
   ```
5. In Jenkins, run:
    - `MB-Bencmark` multibranch job for benchmarks.
    - `test-triggers/child` (if manually triggered) via `Jenkinsfile-child.groovy`.
6. Observe reports and archived artifacts on build pages.

## Troubleshooting & Tips
- Ensure required plugins (`Pipeline`, `Kubernetes`, `C-as-Code`, `Trigger Remote Job`) are installed.
- Review script approvals under **Manage Jenkins → In-Process Script Approval**.
- Check agent labels and Kubernetes cluster connectivity.

## Contributing
- To add a new pipeline:
    1. Create a `Jenkinsfile-<name>.groovy` at repo root.
    2. Add a brief description here and in `README-Pipeline-onboarding.md`.
    3. Test on your Jenkins instance and submit a PR.

## Support
For questions or issues, please contact the DevOps team or open a GitHub issue.