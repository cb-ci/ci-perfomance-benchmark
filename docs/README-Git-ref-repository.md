# Understanding Git Reference Repositories

A **Git reference repository** is essentially a **local cache of Git objects** (commits, trees, blobs, tags, etc.) that other Git clones can reuse instead of downloading everything again from the remote.

---

## 🔧 How it works

* Normally, when you run `git clone https://github.com/org/repo.git`, Git fetches all objects from the remote.
* If you already have another clone of the same repo (or a repo with shared history), you can speed things up by pointing Git to it:

`bash
git clone --reference /path/to/reference/repo https://github.com/org/repo.git new-clone
`

* Git will first try to copy objects from the **reference repo** (fast, local disk).
* Only objects that are missing are fetched from the remote.
* This makes cloning faster and reduces network traffic.

---

## 📦 Use cases

* **CI/CD systems (like Jenkins)**

    * Jenkins can maintain a local bare “reference repository” per remote.
    * All jobs that need to clone that repo can reuse the cache instead of pulling the full history each time.
    * This is configured via the **Git plugin** → *“Advanced clone behaviours”* → *“Use reference repository”*.

* **Large monorepos or frequent clones**

    * Saves both time and bandwidth.
    * Especially useful in environments with slow or rate-limited Git servers (GitHub/GitLab/Bitbucket).

* **Shared development environments**

    * A team can host a reference repo on a shared filesystem; developers clone against it for faster local clones.

---

## ⚠️ Caveats

* If the reference repo is **deleted or corrupted**, clones that rely on it may fail later when accessing missing objects.
* Reference repos don’t get updated automatically — you must keep them in sync with the remote.
* Not all Git hosting providers recommend it (e.g. GitHub suggests shallow clones or caches in CI instead).

---

✅ **Summary**:
A Git reference repository is a **local object cache** that speeds up clones and reduces network usage. In Jenkins, it’s often used so multiple jobs can share one local copy of a Git repo instead of each cloning from GitHub independently.

---

## 🔽 Network traffic reduction

Reference repositories can make a **huge difference** in Enterprise or on-prem Git server environments, especially where many Jenkins jobs or users repeatedly clone the same repos. Let me break it down:

* **Without reference repos**:
  Each clone pulls *all objects* (commits, trees, blobs, tags). For a repo with a few GB of history, 100 Jenkins jobs cloning it = **hundreds of GB** over the network.
* **With reference repos**:
  Jenkins jobs copy objects locally from the cache. The network fetch is only for *new objects* since the last update.

    * First update: full fetch.
    * Subsequent updates: incremental (`git fetch`), often just KB/MB.
* **Typical savings**:

    * **80–95% less network traffic** once the cache is warm.
    * Especially noticeable for large monorepos or many active feature branches.

---

## ⚙️ Workload reduction on Git servers

* **Cloning stress**: Each `git clone` = server must pack and stream the entire repo history. Dozens/hundreds of parallel Jenkins jobs can hammer the server.
* **With reference repos**:

    * Server only serves *incremental fetches* to keep the reference up to date.
    * Jenkins clones copy locally, no server involvement.
* **Impact**:

    * Dramatically fewer packfiles generated.
    * Lower CPU, memory, and I/O on Git server.
    * More predictable performance, fewer slowdowns for developers.

---

## 📊 Example

Imagine:

* A repo = 2 GB history.

* 100 Jenkins jobs cloning in parallel.

* **Without reference repo**: ~200 GB traffic + heavy server CPU for pack generation.

* **With reference repo** (warm cache): maybe 10–50 MB of new objects fetched once, then all 100 jobs clone locally. Network = ~50 MB total.

That’s a **4,000× reduction** in network traffic in this example.

---

## 🚧 Caveats

* You must keep the reference repo updated (e.g. via `git remote update` in a cron/sidecar job).
* If the cache falls behind or corrupts, Jenkins clones may fall back to full fetch.
* Doesn’t help as much if your repos are small or shallow cloned.

---

✅ **Summary**

* **Network traffic**: typically reduced by 80–95%.
* **Git server load**: fewer packfiles, lower CPU/memory usage, much less chance of being overloaded by Jenkins job storms.
* **Best practice**: maintain one local bare reference repo per remote, updated frequently, and point Jenkins jobs at it.

---

## Configuring with JCasC

You can configure a **Git reference repository** for Jenkins using **JCasC (Configuration as Code)**.

### 1. Global Git plugin configuration

If you want to define a global reference repo for all jobs:

`yaml
unclassified:
  gitSCM:
    globalConfigName: "jenkins"
    globalConfigEmail: "jenkins@yourcompany.com"
    useExistingAccount: false
    createAccountBasedOnEmail: false
    referenceRepo: "/var/jenkins_home/git-reference"
`

⚠️ `referenceRepo` here points to a local path on the Jenkins controller or on a mounted volume. Jenkins will use it when cloning repositories.

---

### 2. Multibranch / Pipeline job-level example

If you want to configure reference repos for specific jobs or multibranch pipelines:

`yaml
jobs:
  - script: >
      multibranchPipelineJob('example-mbp') {
        branchSources {
          git {
            id('example-git-source')
            remote('https://github.com/org/repo.git')
            traits {
              cloneOptionTrait {
                extension {
                  shallow(false)
                  noTags(false)
                  reference('/var/jenkins_home/git-reference/repo.git')
                }
              }
            }
          }
        }
      }
`

Here:

* `cloneOptionTrait` is contributed by the **Git plugin**.
* `reference('/path/to/reference/repo.git')` is where you point to the bare repo cache you maintain.

---

### 3. Maintaining the reference repository

Usually you create it like this (bare repo is best):

`bash
mkdir -p /var/jenkins_home/git-reference
cd /var/jenkins_home/git-reference
git clone --mirror https://github.com/org/repo.git
`

And keep it updated (cron or Jenkins job):

`bash
cd /var/jenkins_home/git-reference/repo.git
git remote update --prune
`

---

✅ **Summary**

* `unclassified.gitSCM.referenceRepo` → global default via JCasC.
* `cloneOptionTrait.reference` → job-level or multibranch source.
* Use a **bare mirror repo** as the reference; update it regularly.

---

## Scalable Design for 10k–50k+ Repos

Short answer: you’re right—**a blind “fetch everything every 120s” loop won’t scale** to 50k repos. You’ll hammer your Git server and still risk short windows where a webhook arrives before the cache is fresh.

Here’s a pattern that fixes both problems.

### 1) Event-driven, not interval-driven

* **Stop global polling.** Don’t “update all mirrors every N seconds.”
* **Use webhooks to target a single repo** when something actually changed.
* **Queue + rate-limit** those updates so you never exceed safe QPS against your Git server.

#### Minimal flow

1. Git server sends webhook (push/PR/merge/tag) →
2. A tiny HTTP receiver enqueues “`org/repo` changed” →
3. A worker sidecar drains the queue and runs `git remote update --prune` **for that repo only**, with a token-bucket **rate limiter** and **max concurrency** (e.g., 5–10 parallel fetches).

> Result: You update **only what changed**, and you control load precisely.

### 2) Hot-set cache, not 50k mirrors

* Maintain mirrors only for the **hot repos** (recently built or recently changed).
* Track last-accessed timestamps; keep, say, **5–10k hottest mirrors**; **evict** cold ones (delete their mirror folders) to save space and O&M.
* On demand, if a cold repo triggers, seed its mirror once, then it joins the hot set.

### 3) Webhooks vs. build freshness (no race)

Even with event-driven updates, a build might start **before** the mirror refresh finishes. That’s ok if you configure clones like this:

* Jenkins clone uses **both**:

    * `--reference /var/jenkins_home/git-reference/<name>.git` (fast local objects **when available**)
    * **remote = origin** (authoritative)
* If the reference mirror **doesn’t yet** have the latest objects, **Git automatically fetches them from origin**. The build **never waits** for the mirror. The mirror update just reduces bandwidth for the *next* builds.

### 4) Further load controls

* **Per-repo backoff/jitter:** if a repo is very chatty, back off updates (e.g., coalesce events for 30–60s).
* **Cap total RPS and concurrency:** token bucket (e.g., 1 fetch/sec, burst 5; 5–10 workers max).
* **Skip no-op fetches:** use protocol v2 (default on modern Git) which negotiates efficiently; optionally filter tags (`--no-tags`) if not needed.
* **GC sparingly:** run `git gc` daily/weekly, **not** on every update.

---

## Kubernetes Reference Implementation

### A) Tiny webhook receiver (sidecar) → writes to a queue

Use a trivial HTTP server that writes one file per repo into a **work queue directory**.

`yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: refcache-webhook
data:
  server.py: |
    #!/usr/bin/env python3
    import os, json, time
    from http.server import BaseHTTPRequestHandler, HTTPServer
    QUEUE = os.environ.get("QUEUE_DIR", "/queue")
    class H(BaseHTTPRequestHandler):
        def do_POST(self):
            try:
                length = int(self.headers.get('content-length', 0))
                body = self.rfile.read(length).decode('utf-8')
                payload = json.loads(body) if body else {}
                # Extract "org/repo" robustly (adjust per your Git server)
                full = (payload.get("repository", {}) or {}).get("full_name") or \
                       (payload.get("project", {}) or {}).get("path_with_namespace")
                if not full:
                    self.send_response(202); self.end_headers(); return
                # coalesce: touch/update one file per repo
                path = os.path.join(QUEUE, full.replace("/", "__"))
                open(path, "w").write(str(time.time()))
                self.send_response(202); self.end_headers()
            except Exception:
                self.send_response(202); self.end_headers()
    if __name__ == "__main__":
        os.makedirs(QUEUE, exist_ok=True)
        HTTPServer(("0.0.0.0", 8080), H).serve_forever()
`

Expose it behind your ingress and configure your Git server’s webhook to POST to `/`.

### B) Worker sidecar (rate-limited, hot-set, on-demand)

This reads the queue directory, rate-limits, and updates mirrors for only those repos.

`yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: refcache-worker
data:
  worker.sh: |
    #!/usr/bin/env bash
    set -euo pipefail
    QUEUE_DIR="${QUEUE_DIR:-/queue}"
    ROOT="${REPO_ROOT:-/var/jenkins_home/git-reference}"
    CONCURRENCY="${CONCURRENCY:-5}"
    QPS="${QPS:-1}"                 # max fetches/sec
    HOT_TTL_DAYS="${HOT_TTL_DAYS:-30}" # evict mirrors unused for N days
    mkdir -p "$ROOT" "$QUEUE_DIR"

    # run one fetch; uses HTTPS token or SSH if configured in env
    fetch_one() {
      local full="$1" name="${full//\//__}" dir="$ROOT/${name}.git" url
      url="$(/bin/sh -c "echo \${URL__${name}}")"
      if [[ -z "${url}" ]]; then
        echo "[warn] URL for $full not configured"; return 0
      fi
      if [[ ! -d "$dir" ]]; then
        echo "[seed] $full ← $url"
        git clone --mirror "$url" "$dir"
      fi
      echo "[update] $full"
      git -C "$dir" remote set-url origin "$url"
      git -C "$dir" remote update --prune --no-tags
      # touch access file for hot-set tracking
      date +%s > "$dir/.last_access"
    }

    # simple token-bucket via sleep
    throttle() { sleep "$(awk "BEGIN{print 1/${QPS}}")"; }

    export -f fetch_one throttle
    while true; do
      # coalesce: process queue files, limited concurrency
      ls -1 "$QUEUE_DIR" 2>/dev/null | head -n 1000 | \
      xargs -I{} -P "${CONCURRENCY}" bash -c '
        full="${1//__/\/}"; file="${2}"
        rm -f "${file}"
        fetch_one "${full}"
        throttle
      ' _ "{ }" "$QUEUE_DIR/{ }" || true

      # periodic hot-set eviction
      find "$ROOT" -maxdepth 1 -type d -name "*.git" -print0 | while IFS= read -r -d '' d; do
        last=$(cat "$d/.last_access" 2>/dev/null || echo 0)
        now=$(date +%s)
        age_days=$(( (now - last) / 86400 ))
        if (( age_days > HOT_TTL_DAYS )); then
          echo "[evict] $(basename "$d") age=${age_days}d"
          rm -rf "$d" || true
        fi
      done
      sleep 2
    done
`

### C) Environment mapping (repo → URL)

To avoid keeping 50k entries in env, mount a **ConfigMap** (or file) keyed by repo name. Example env for a few top repos:

`yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: refcache-urls
data:
  env: |
    # map org/repo → remote
    export URL__org__repo=https://github.com/org/repo.git
    export URL__org2__bigrepo=git@github.com:org2/bigrepo.git
`

### D) Pod spec snippets (controller pod)

Mount the shared PVC (`jenkins-home`) for both sidecars so they can update `/var/jenkins_home/git-reference`.

`yaml
spec:
  volumes:
    - name: jenkins-home
      persistentVolumeClaim:
        claimName: jenkins-home-pvc
    - name: queue
      emptyDir: {}  # in-pod queue
    - name: refcache-webhook
      configMap: { name: refcache-webhook, defaultMode: 0755 }
    - name: refcache-worker
      configMap: { name: refcache-worker,  defaultMode: 0755 }
    - name: refcache-urls
      configMap: { name: refcache-urls }

  containers:
    - name: refcache-webhook
      image: python:3.12-alpine
      command: ["/bin/sh","-c","chmod +x /app/server.py && exec /app/server.py"]
      ports: [{containerPort: 8080}]
      env: [{ name: QUEUE_DIR, value: /queue }]
      volumeMounts:
        - { name: jenkins-home, mountPath: /var/jenkins_home }
        - { name: queue,        mountPath: /queue }
        - { name: refcache-webhook, mountPath: /app }

    - name: refcache-worker
      image: alpine/git:2.45.2
      command: ["/bin/sh","-c","source /env/env && chmod +x /app/worker.sh && exec /app/worker.sh"]
      env:
        - { name: REPO_ROOT, value: /var/jenkins_home/git-reference }
        - { name: QUEUE_DIR, value: /queue }
        - { name: CONCURRENCY, value: "8" }
        - { name: QPS, value: "1" }            # <= rate limit here
        - { name: HOT_TTL_DAYS, value: "30" }
        # HTTPS token or SSH settings can be added here
      volumeMounts:
        - { name: jenkins-home,   mountPath: /var/jenkins_home }
        - { name: queue,          mountPath: /queue }
        - { name: refcache-worker, mountPath: /app }
        - { name: refcache-urls,  mountPath: /env }

    # (Your jenkins controller container is also here, mounting jenkins-home)
`

Expose `refcache-webhook` via a Service/Ingress and point your Git server’s webhook at it.

---

## Jenkins side settings to avoid races

* Keep using `--reference /var/jenkins_home/git-reference/<name>.git`.
* Do **not** disable network access: let Git fetch missing objects from origin if the mirror isn’t up to date yet.
* Optionally, add a **pre-checkout step** in pipelines:

  `groovy
  stage('Prepare') {
    steps {
      sh 'git -C "$WORKSPACE" fetch --no-tags origin +refs/heads/*:refs/remotes/origin/* --depth=1 || true'
    }
  }
  `

  (Generally not required; the standard checkout step will fetch what’s missing.)

---

## Why this works

* **No flood:** You touch the Git server **only when events occur**, and even then with **strict rate limits** and **bounded concurrency**.
* **Fresh enough:** Webhook triggers the cache update *immediately* for that repo; if the build beats the update, the clone still succeeds by fetching missing objects from origin.
* **Cost-aware:** You store mirrors only for **hot repos** and evict cold ones automatically.
* **Simple ops:** All logic is in two tiny sidecars with a file-based queue—no external infra required (but you can swap the queue for SQS/Kafka later if you like).

---

## Gerrit Integration

Yes — **Gerrit changes the dynamics quite a bit compared to “just” running 50 000 repos on a plain Git server**.

### 🔑 How Gerrit helps

1. **Single repository model**

    * Gerrit is usually run as a *central repo of record*.
    * All developers and automation clone/fetch from Gerrit, not directly from GitHub/Bitbucket.
    * Jenkins (or any CI) sees one consistent remote.
    * You can configure Gerrit to serve *many projects* from one instance, but with unified access control, hooks, and replication.

2. **Smart server-side caching**

    * Gerrit uses **persistent JGit caches** for objects, packs, refs, etc.
    * This drastically reduces CPU and I/O overhead compared to vanilla Git daemons serving many parallel `clone`s.
    * With 50 000 projects, that’s critical: Gerrit reuses packfiles across requests, instead of regenerating them per fetch.

3. **Event stream instead of polling**

    * Gerrit emits **events** (`patchset-created`, `ref-updated`, etc.) over an event stream or via plugins (Kafka, AMQP).
    * Jenkins can subscribe directly, so you don’t need to poll 50 000 refs or run periodic `git fetch`.
    * This solves the “update every 120s” flood problem.

4. **Replication plugin**

    * Gerrit can replicate repositories to multiple downstream Git mirrors.
    * You can place mirrors closer to CI or in multiple regions, reducing latency and spreading load.
    * Jenkins can then point its reference repositories to the nearest mirror, not the central server.

5. **Granular access control**

    * You can limit what Jenkins sees (e.g. specific refs/namespaces) to reduce clone size and noise.
    * Combined with shallow clones or narrow refspecs, this lowers I/O dramatically.

---

### ⚠️ What Gerrit does *not* magically solve

* **Disk/storage scaling**: 50 000 repos still need storage and periodic garbage collection. Gerrit helps with efficiency, but the data volume remains.
* **Reference repository freshness**: If Jenkins still relies on local mirrors, they need updating. Gerrit’s events help trigger updates smarter, but you still need a sync mechanism.
* **Operational complexity**: Gerrit itself is a heavy service (Java, JGit, replication daemons, database backend). Running it at 50k-repo scale is non-trivial.

---

### ✅ Summary

* A plain Git server will drown if you try to serve 50k repos with naive periodic reference-repo updates.
* Gerrit helps by:

    * providing **event-driven updates** (so Jenkins only reacts to changes),
    * caching packfiles **server-side** (reducing CPU per clone/fetch),
    * supporting **replication** (distribute load).
* You still need to architect the CI side carefully (e.g. hot-set reference repos, event-driven sync, concurrency limits), but Gerrit makes it *much* more sustainable.
