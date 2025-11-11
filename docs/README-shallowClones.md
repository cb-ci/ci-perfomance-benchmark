Here’s the quick, CI-focused rundown.

# What is a shallow clone?

A **shallow clone** copies only the *most recent* part of a repo’s history instead of the full commit graph:

* `git clone --depth 50` → fetch the latest 50 commits reachable from the checkout target.
* You can also shallow on fetch: `git fetch --depth 50`.
* You can later **deepen** or **unshallow**: `git fetch --deepen=200` or `git fetch --unshallow`.

It keeps the *working tree* (your files) the same as normal; it just limits what’s in `.git/objects` (history).

# Why it matters (network & speed)

* **Much less to transfer:** fewer commits, trees, and deltas → smaller packfiles over the wire → **faster clone/indexing** in CI.
* **Faster subsequent fetches:** Jenkins multibranch/Org Folder jobs fetch frequently; with shallow history the incremental updates are smaller.
* **Lower Git server load:** less bandwidth and CPU for pack negotiation, especially when many controllers/agents are cloning concurrently.

# Why it matters (workspace size)

* **.git is smaller:** the object database shrinks dramatically because it doesn’t store deep history.
* **Working tree size is unchanged:** you still materialize the files for the checked-out commit. (To shrink file payloads themselves, see “partial clone” below.)

# Jenkins best practices

* **Enable shallow clone on the job/trait:**

    * Git plugin / traits:

      ```yaml
      - cloneOption:
          extension:
            cloneOption:
              shallow: true
              depth: 50      # tune: 20–200 is common
              noTags: true   # skip tags unless you need them
              timeout: 30
      ```
    * For submodules, also set **submodule shallow** if possible.
* **Build PR merges?** Make sure the depth includes the merge commit your job checks out. If you ever hit “requested commit not found,” deepen:

  ```bash
  git fetch --deepen=500 || git fetch --unshallow
  ```
* **Versioning from tags?** If your pipeline uses `git describe`, GitVersion, release plugins, etc., you’ll likely need tags or full history. Either:

    * temporarily fetch tags: `git fetch --tags --deepen=200`, or
    * disable shallow for those jobs.

# When shallow is not a good fit

* Pipelines that **compute versions from tags/history** (e.g., `git describe`, semantic-versioning tools).
* Jobs that **diff/merge across long ranges** (e.g., `git log main...feature` when `main` isn’t in shallow range).
* Release tasks that **create tags** or need ancestry checks.
* Some **LFS-heavy** repos: shallow does not reduce LFS payload; manage LFS separately (e.g., `GIT_LFS_SKIP_SMUDGE=1` and fetch artifacts you actually need).

# Related options (and how they differ)

* **Partial clone** (`--filter=blob:none`): fetches *no* file blobs initially—downloads blobs on demand. This can reduce both **network** and **working tree** I/O dramatically, but requires server support and client Git ≥ 2.22. Great for very large monorepos. (Jenkins Git plugin support varies; you can still use it in shell steps.)
* **Sparse checkout**: limits *which paths* are populated in the working tree. Combine with shallow or partial for maximum savings.

# Practical tuning tips

* Start with `depth: 50` (often enough for PR/branch builds). If you see “commit not found,” bump to 200.
* Set `noTags: true` unless your pipeline needs tags.
* For **Org Folder / Multibranch** at scale, shallow clones + webhook-first scans give you big wins in both **index time** and **GitHub/GitLab load**.
* Be consistent: mixing shallow and full clones across similar jobs makes cache behavior and troubleshooting harder.
