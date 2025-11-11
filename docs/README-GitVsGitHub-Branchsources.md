Good question — in Jenkins/CloudBees CI there’s a **big difference between using the plain Git Branch Source vs. the GitHub Branch Source** in a Multibranch Pipeline (or Organization Folder).

Here’s a breakdown:

---

### **1. Git Branch Source (SCM = `git`)**

* **Generic Git**: Works with *any* Git server (GitHub, GitLab, Bitbucket, Gerrit, enterprise Git servers, on-prem, etc.).
* **Discovery**: Limited. It can only discover branches (and optionally tags) by cloning/fetching from the remote. It does not have native PR/MR awareness.
* **Pull Requests**:

    * No native PR handling.
    * You must manually script fetching refs (like `refs/pull/*/head`) or configure advanced refspecs if you want PRs.
* **Webhooks**: Requires you to manually configure generic webhook payloads and URL triggers. No special SCM API integration.
* **SCM Checks/Statuses**: Cannot send commit status updates back to GitHub (success/failure checks).

**Use case**: You want Jenkins to talk to *any* Git server, not just GitHub, or you have very simple needs (only branches/tags, no PR automation).

---

### **2. GitHub Branch Source (SCM = `github`)**

* **GitHub-specific plugin**: Adds a layer on top of `git` with GitHub API integration.
* **Discovery**: Can discover branches *and* Pull Requests (open, merged, origin vs. forked).
* **Traits**: Gives you fine-grained discovery strategies (discover PRs from origin, PRs from forks, whether to build head or merged commit, etc.).
* **Webhooks**: Automatically registers GitHub webhooks when using a GitHub App or personal access token. Jenkins knows how to interpret events like `push`, `pull_request`, etc.
* **SCM Checks API**: With the SCM Reporting / Checks plugins, Jenkins can update the GitHub Checks UI with build results, test summaries, logs, etc.
* **Rate limits**: Uses GitHub REST API (or GraphQL) for indexing and discovery. With GitHub App authentication, this scales much better than tokens.
* **Security**: GitHub App auth also gives fine-grained, revocable permissions instead of wide PAT access.

**Use case**: You are on GitHub (cloud or enterprise), want to build PRs automatically, report build status back to GitHub, and benefit from rich GitHub integration.

---

### **Summary Table**

| Feature                    | Git Branch Source           | GitHub Branch Source                   |
| -------------------------- | --------------------------- | -------------------------------------- |
| Server support             | Any Git server              | GitHub only                            |
| Branch discovery           | ✅                           | ✅                                      |
| Tag discovery              | ✅                           | ✅                                      |
| Pull Requests              | ❌ (manual refs)             | ✅ (origin + forks, merged/head builds) |
| Webhooks auto-registration | ❌                           | ✅                                      |
| Commit status / Checks API | ❌                           | ✅                                      |
| Authentication             | Basic Git creds (SSH/HTTPS) | GitHub App, PAT, OAuth                 |
| Rate-limit handling        | N/A                         | Optimized with GitHub App              |

---

👉 So the main difference is:

* **Git Branch Source = low-level, generic, minimal integration.**
* **GitHub Branch Source = GitHub-aware, PR-aware, webhook-aware, checks-aware.**

