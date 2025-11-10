# Controller StressNG Tests

This document outlines an approach to stress testing CloudBees CI Controllers and Agents, with a focus on Kubernetes and ephemeral pod agents.

## Table of Contents

- [Objective](#objective)
- [Prerequisites](#prerequisites)
- [Building the Custom Image](#building-the-custom-image)
  - [Build Process](#build-process)
  - [Dockerfile](#dockerfile)
- [Usage](#usage)
  - [Update Kubernetes Deployment](#update-kubernetes-deployment)
  - [Manual Testing](#manual-testing)
- [`stress-ng` Commands](#stress-ng-commands)
  - [`stress-ng` Options](#stress-ng-options)
  - [Example Commands](#example-commands)
- [Troubleshooting](#troubleshooting)
- [Additional Resources](#additional-resources)

## Objective

The primary objective is to simulate various load conditions on a CloudBees CI Managed Controller to identify performance bottlenecks and ensure stability. This is achieved by using the `stress-ng` utility, which can subject the system to a wide range of stressors, including CPU, memory, I/O, and network load.

To use `stress-ng`, it must be available within the controller's container. This is accomplished by building a custom Docker image based on the official CloudBees CI image.

## Prerequisites

Before you begin, ensure you have the following:

*   **Docker:** Installed and configured on your machine.
*   **CloudBees CI Base Image:** Access to a CloudBees CI base image. The Dockerfile uses `cloudbees/cloudbees-core-mm:latest-jdk21` as an example. You should replace this with the version appropriate for your environment.
*   **Kubernetes CLI (`kubectl`):** Required for managing the deployment of the controller.

## Building the Custom Image

A custom Docker image is created that includes the `stress-ng` tool.

### Build Process

The custom image is built using a multi-stage Dockerfile.

1.  **Build Stage:** A Red Hat Universal Base Image (UBI) is used as a build environment. `stress-ng` is compiled from source in this stage. This approach was chosen over installing from a package manager to avoid potential compatibility issues with the base image's repositories.
2.  **Final Image:** The compiled `stress-ng` binary and its dependencies are copied from the build stage to the final CloudBees CI image.

To build and push the image, run the provided shell script:

```bash
./dockerbuild.sh
```

The script will build the image with the name `caternberg/cb-ci-controller-stress-ng:latest` and push it to Docker Hub. You can customize the image name and tag by editing the `dockerbuild.sh` script.

### Dockerfile

```dockerfile
FROM cloudbees/cloudbees-core-mm:latest-jdk21 AS base

FROM registry.access.redhat.com/ubi8:8.10 AS build

COPY --from=base / /cbroot

RUN dnf update -y && \
    dnf install -y git gcc make procps-ng && \
    git clone https://github.com/ColinIanKing/stress-ng.git /tmp/stress-ng && \
    make -C /tmp/stress-ng && \
    make -C /tmp/stress-ng install DESTDIR=/cbroot

FROM base
COPY --from=build /cbroot/ /
```

## Usage

After building and pushing the custom image, you need to update your CloudBees CI Managed Controller to use it.

### Update Kubernetes Deployment

To use the custom image, you need to update the `image` field in your Kubernetes deployment or StatefulSet manifest for the Managed Controller.

Example of a partial manifest:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: cjoc
spec:
  template:
    spec:
      containers:
        - name: jenkins
          image: caternberg/cb-ci-controller-stress-ng:latest
          # ... other container configuration
```

Apply the updated manifest to your Kubernetes cluster.

### Manual Testing

For manual testing, you can get a shell into the running controller pod and execute `stress-ng` commands directly.

1.  Find the pod name of your controller:
    ```bash
    kubectl get pods -l com.cloudbees.cje.kind=managed-master
    ```

2.  Get a shell into the pod:
    ```bash
    kubectl exec -it <pod-name> -- /bin/bash
    ```

3.  Run `stress-ng` commands:
    ```bash
    stress-ng --cpu 1 --timeout 60s
    ```

## `stress-ng` Commands

Here are some example `stress-ng` commands that can be executed in a shell step within a Jenkins job running on the controller.

### `stress-ng` Options

| Option | Meaning |
|---|---|
| `--cpu 2` | Runs 2 CPU stressors |
| `--io 2` | Runs 2 I/O stressors |
| `--vm 2` | Runs 2 memory stressors |
| `--vm-bytes 2048M` | Each `--vm` stressor allocates 2048 MB of RAM |
| `--sock 2` | Starts 2 socket stressors |
| `--hdd 1` | Starts 1 disk I/O stressor |
| `--timeout 60s` | Total test duration = 60 seconds |
| `--metrics-brief` | Outputs a summary of resource usage and performance |

### Example Commands

*   **CPU Stress:**
    ```bash
    stress-ng --cpu 4 --cpu-load 80 --timeout 5m
    ```
*   **Memory Stress:**
    ```bash
    stress-ng --vm 2 --vm-bytes 75% --timeout 5m
    ```
*   **Disk I/O Stress:**
    ```bash
    stress-ng --hdd 2 --hdd-bytes 2G --timeout 5m
    ```
*   **Mixed Stress:**
    ```bash
    stress-ng --cpu 2 --io 2 --vm 2 --hdd 1 --timeout 10m
    ```

## Troubleshooting

*   **Docker Build Fails:** If the Docker build fails, check the output for errors. Common issues include network problems (e.g., failing to clone the `stress-ng` repository) or incompatibilities with the base image. Ensure you have a working internet connection and that the base image is accessible.
*   **Permission Issues:** If you encounter permission issues when running `dockerbuild.sh`, ensure the script is executable (`chmod +x dockerbuild.sh`) and that your user has the necessary permissions to run Docker commands.

## Additional Resources

*   [stress-ng GitHub Repository](https://github.com/ColinIanKing/stress-ng)
*   [CloudBees CI Documentation](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/)
