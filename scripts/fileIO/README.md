

*very* relevant for Jenkins performance engineering, especially when evaluating storage backends for `$JENKINS_HOME` (e.g., local SSD, EFS/NFS, or PD-SSD on GKE).

Let’s break it down step-by-step — you’ll see why **random I/O** and **sequential I/O** matter differently for Jenkins controllers, agents, and caches 👇

---

## 🧩 1. The two fundamental I/O patterns

| Type               | Description                                             | Typical block size           | Access pattern         | Analogy                                 |
| ------------------ | ------------------------------------------------------- | ---------------------------- | ---------------------- | --------------------------------------- |
| **Sequential I/O** | Data is read/written in *order* (contiguous blocks)     | Usually large (64 KB – 1 MB) | Linear                 | Copying a large file or streaming video |
| **Random I/O**     | Data is read/written from *scattered locations* on disk | Usually small (4 KB – 64 KB) | Non-linear, seek heavy | Reading lots of tiny files at once      |

---

## ⚙️ 2. How it affects performance

| Metric                           | Sequential I/O              | Random I/O                               |
| -------------------------------- | --------------------------- | ---------------------------------------- |
| **IOPS (operations per second)** | Usually lower importance    | Very important — small ops dominate      |
| **Throughput (MB/s)**            | Typically very high         | Lower due to seek overhead               |
| **Latency (ms/op)**              | Low and stable              | Highly variable                          |
| **Best for**                     | Big file transfers, backups | Metadata-heavy workloads (like Jenkins!) |

---

## 💡 3. How this maps to Jenkins workloads

| Jenkins Component                     | I/O Pattern                            | Explanation                                                                                                                                      |
| ------------------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| **$JENKINS_HOME** (controller)        | 🔴 Mostly **random I/O (small files)** | Jenkins constantly writes metadata, XMLs, build logs, fingerprints, queue state — thousands of small files. Random I/O and latency are critical. |
| **Build workspaces (agent volumes)**  | 🟢 **Sequential + random mix**         | Builds extract sources, compile (sequential writes), then produce logs and artifacts (mixed).                                                    |
| **Artifact storage (shared volumes)** | 🟢 **Sequential I/O**                  | Large artifacts are uploaded/downloaded linearly.                                                                                                |
| **Caches (e.g. Maven, npm, Gradle)**  | 🟠 Random + metadata-heavy             | Many small lookups; performance depends on latency and metadata ops.                                                                             |

---

## 🧠 4. Example from FIO benchmarking

| Pattern               | FIO flags                        | What it simulates                | Relevance                     |
| --------------------- | -------------------------------- | -------------------------------- | ----------------------------- |
| Sequential read/write | `--rw=read` or `--rw=write`      | Copying big artifacts or backups | Tests throughput (MB/s)       |
| Random read/write     | `--rw=randrw` or `--rw=randread` | Jenkins metadata access          | Tests latency + IOPS          |
| Mixed workload        | `--rw=randrw --rwmixread=70`     | 70% reads, 30% writes            | Closer to Jenkins’ real world |

---

## 🚀 5. Typical performance comparison (PD-SSD vs. NFS)

| Storage                  | Sequential MB/s                     | Random IOPS (4 K)                      | Latency (ms) | Jenkins impact                        |
| ------------------------ | ----------------------------------- | -------------------------------------- | ------------ | ------------------------------------- |
| **Local SSD / NVMe**     | 🔥 400 – 2000 MB/s                  | 🔥 50 000 – 200 000                    | 0.2–0.5      | Excellent for controller & build I/O  |
| **GCE PD-SSD (block)**   | ⚙️ 100–600 MB/s                     | ⚙️ 10 000–30 000                       | 1–3          | Good balance                          |
| **EFS / NFS (network)**  | ❄️ 50–200 MB/s                      | ❄️ 500–3000                            | 5–20         | Poor for controller; ok for artifacts |
| **EBS gp3 (AWS)**        | ⚙️ Up to 16 000 IOPS (configurable) | ⚙️ ~64 MB/s per 3000 IOPS              | 1–2          | Good for controller                   |
| **FSx for ZFS / Lustre** | 🔥 High throughput                  | ⚙️ Good random I/O (depends on config) | <2           | Excellent shared storage if tuned     |

---

## 💬 6. TL;DR for Jenkins use cases

| Jenkins Component                   | Recommended I/O Type     | Storage Recommendation               |
| ----------------------------------- | ------------------------ | ------------------------------------ |
| **Controller (`$JENKINS_HOME`)**    | Random I/O (low latency) | Local SSD, PD-SSD, EBS (gp3/gp2)     |
| **Build Agents (workspace)**        | Sequential + random      | Node local SSD or ephemeral PV       |
| **Shared artifact storage / cache** | Sequential               | Object store (S3/GCS), or FSx/Lustre |
| **Backup / archive**                | Sequential               | NFS, GCS, S3, EFS fine here          |

---

## 🧪 7. Sample FIO commands

### 🔹 Sequential write test:

```bash
fio --name=seqwrite --ioengine=libaio --rw=write --bs=1M --size=2G \
    --numjobs=1 --runtime=30 --filename=/mnt/testfile --group_reporting
```

### 🔹 Random read/write test (Jenkins-like):

```bash
fio --name=randrw --ioengine=libaio --rw=randrw --bs=4k --size=2G \
    --numjobs=8 --iodepth=16 --runtime=60 --filename=/mnt/testfile --group_reporting
```

Compare **IOPS, latency, and throughput** between your local SSD, NFS/EFS, or PD-SSD.

---

## 🧭 8. Key takeaway

> Jenkins controllers are **latency-sensitive** and dominated by **small random I/O**.
> Always test storage using **random 4K patterns**, not just sequential throughput.

---


# FIO Pod

Here’s a **ready-to-run Kubernetes Pod manifest** that runs an **`fio` benchmark** directly on your **PersistentVolumeClaim** (PVC).
It’s safe, clean, and follows the best-practice test pattern for block-level I/O benchmarking on Kubernetes.

---

## 🧩 **Step 1: Prerequisites**

Make sure you already have:

* a **PVC** bound to your `pd-ssd` StorageClass, e.g.

  ```bash
  kubectl get pvc
  NAME               STATUS   VOLUME                                     CAPACITY   STORAGECLASS         AGE
  jenkins-home       Bound    pvc-xxxx-xxxx                             100Gi      ssd-cloudbees-ci-cjoc1   5d
  ```

If not, create one first:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: fio-test-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: ssd-cloudbees-ci-cjoc1
```

---

## 🚀 **Step 2: Launch the FIO test Pod**

Save this as `fio-test-pod.yaml` and apply it:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: fio-benchmark
  namespace: default
spec:
  restartPolicy: Never
  containers:
  - name: fio
    #image: alpine/fio:latest
    image: aseebh/fio:alpine
    command: ["sh", "-c"]
    args:
      - |
        echo ">>> Running fio benchmark on mounted pd-ssd PVC"
        fio --name=randrw \
            --ioengine=libaio \
            --direct=1 \
            --rw=randrw \
            --bs=4k \
            --numjobs=8 \
            --iodepth=16 \
            --size=2G \
            --runtime=60 \
            --time_based \
            --filename=/mnt/testfile \
            --group_reporting
        echo ">>> Done"
    volumeMounts:
    - name: test-volume
      mountPath: /mnt
  volumes:
  - name: test-volume
    persistentVolumeClaim:
      claimName: fio-test-pvc
```

Apply:

```bash
kubectl apply -f fio-test-pod.yaml
```

Then monitor logs:

```bash
kubectl logs -f fio-benchmark
```

---

## 📊 **Expected Results on `pd-ssd` (100 GB+)**

| Metric                   | Expected Range     |
| ------------------------ | ------------------ |
| Random 4K Read IOPS      | 3,000 – 6,000 IOPS |
| Random 4K Write IOPS     | 2,000 – 5,000 IOPS |
| Avg Latency              | 0.6 – 1.5 ms       |
| Throughput (16 K / 64 K) | 50 – 250 MB/s      |

---

## 🧰 **Optional: Test Variants**

### 🔹 Sequential read/write (throughput test)

```bash
fio --name=seqwrite --rw=write --bs=1M --size=4G --numjobs=4 --direct=1 --ioengine=libaio --filename=/mnt/testfile
```

### 🔹 Random read/write (latency/IOPS test)

```bash
fio --name=randrw --rw=randrw --bs=4k --numjobs=8 --iodepth=16 --direct=1 --time_based --runtime=60 --size=2G --filename=/mnt/testfile
```

---

## Notes

Uses alpine/fio:latest → lightweight and always available from Docker Hub.

You can change the workload type easily:
```
--rw=read        # sequential read
--rw=write       # sequential write
--rw=randread    # random read
--rw=randwrite   # random write
```


Adjust `--bs`, `--numjobs`, or `--iodepth` for larger or smaller workloads.

Runs safely in cjoc1 namespace alongside CloudBees CI.
---

## 💡 **Cleanup**

After you’re done:

```bash
kubectl delete pod fio-benchmark
kubectl delete pvc fio-test-pvc
```

---


# 504 Nginx error (or equivalent)

Avoid 504

NGINX Ingress (Kubernetes)

Add annotations to your Ingress manifest:

> kubectl edit ing <controller>

```
metadata:
  annotations:
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-body-size: "100m"

```