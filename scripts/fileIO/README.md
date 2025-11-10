# FIO Pod

Here’s a **ready-to-run Kubernetes Pod manifest** that runs an **`fio` benchmark** directly on your **GCP `pd-ssd` PersistentVolumeClaim** (PVC).
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
    image: alpine/fio:latest
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