# GitHub Branch Source: Recommended Setup & Pitfalls (CloudBees CI)

This document outlines the recommended setup for **GitHub Branch Source** in Multibranch and Organization projects, along with common pitfalls to avoid.

## Table of Contents

* [Quick Links](#quick-links)
* [GitHub Organizations – Key Limits & Guidance](#github-organizations--key-limits--guidance)
* [Multibranch Lifecycle Hygiene](#multibranch-lifecycle-hygiene)
* [Recommended Defaults (Most Teams)](#recommended-defaults-most-teams)
* [Settings to Consider (Case-by-Case)](#settings-to-consider-case-by-case)
* [Settings to Avoid (In Most Orgs)](#settings-to-avoid-in-most-orgs)
* [Git Hygiene & Performance](#git-hygiene--performance)
* [Focused JCasC Edits (diff-style)](#focused-jcasc-edits-diff-style)
* [Observability & Troubleshooting](#observability--troubleshooting)
* [Typical Issues to Watch](#typical-issues-to-watch)
* [TL;DR](#tldr)

---

## Quick Links

**CloudBees**

* KB: Git reference repository – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/client-and-managed-controllers/how-to-create-and-use-a-git-reference-repository)
* KB: Prevent initial index builds – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/troubleshooting-guides/prevent-multibranch-organization-initial-index-builds)
* Build Strategies (admin guide) – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/cloudbees-build-strategies-plugin)
* GitHub App authentication – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-app-auth)
* GitHub Branch Source (plugin docs) – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin)
* Webhooks & triggers – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin#_using_build_triggers_and_webhooks)
* Multibranch template syntax (examples) – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/examples)
* Multibranch template syntax (GitHub) – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/github)
* Traditional admin guide (Build Strategies) – [docs.cloudbees.com](https://docs.cloudbees.com/docs/cloudbees-ci/latest/traditional-admin-guide/cloudbees-build-strategies-plugin)
* Build Strategies (release notes) – [docs.cloudbees.com](https://docs.cloudbees.com/docs/release-notes/latest/plugins/cloudbees-build-strategies-plugin/)
* Basic Branch Build Strategies (plugin site) – [docs.cloudbees.com](https://docs.cloudbees.com/plugins/ci/basic-branch-build-strategies)
* Multibranch Build Strategy Extension – [docs.cloudbees.com](https://docs.cloudbees.com/plugins/ci/multibranch-build-strategy-extension)
* Blog: Weathering build storms – [cloudbees.com](https://www.cloudbees.com/blog/weathering-build-storms-in-the-enterprise)

**Jenkins (plugins & Javadoc)**

* Change Request strategy Javadoc – [javadoc.jenkins.io](https://javadoc.jenkins.io/plugin/basic-branch-build-strategies/jenkins/branch/buildstrategies/basic/ChangeRequestBuildStrategyImpl.html)
* Tag strategy Javadoc – [javadoc.jenkins.io](https://javadoc.jenkins.io/plugin/basic-branch-build-strategies/jenkins/branch/buildstrategies/basic/TagBuildStrategyImpl.html)
* Fork PR trust policy Javadoc – [javadoc.jenkins.io](https://javadoc.jenkins.io/plugin/github-branch-source/org/jenkinsci/plugins/github_branch_source/ForkPullRequestDiscoveryTrait.GitHubForkTrustPolicy.html)
* Basic Branch Build Strategies (plugin index) – [plugins.jenkins.io](https://plugins.jenkins.io/basic-branch-build-strategies/)
* GitHub Branch Source plugin repo – [github.com/jenkinsci/github-branch-source-plugin](https://github.com/jenkinsci/github-branch-source-plugin)
* Security & default permissions strategies – [github.com (doc)](https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies)

**GitHub (platform)**

* API root – [api.github.com](https://api.github.com/)
* Org admin strategies – [resources.github.com](https://resources.github.com/learn/pathways/administration-governance/essentials/strategies-for-using-organizations-github-enterprise-cloud/)
* Org startegies - See https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies
* Registering a GitHub App – [docs.github.com](https://docs.github.com/en/apps/creating-github-apps/registering-a-github-app/registering-a-github-app)
* Installing a GitHub App (Marketplace/org) – [docs.github.com](https://docs.github.com/en/apps/using-github-apps/installing-a-github-app-from-github-marketplace-for-your-organizations)
* SAML / team sync – [docs.github.com](https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization)
* Team sync usage limits – [docs.github.com](https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization#usage-limits)
* About teams – [docs.github.com](https://docs.github.com/en/organizations/organizing-members-into-teams/about-teams)
* Repository limits – [docs.github.com](https://docs.github.com/en/repositories/creating-and-managing-repositories/repository-limits)
* Helper script: list SCM pollings – [github.com/cb-ci/ci-groovy](https://github.com/cb-ci/ci-groovy/blob/main/printAllSCMPollings.groovy)

**Community references**

* DSL discovery modes (Stack Overflow) – [stackoverflow.com](https://stackoverflow.com/questions/67871598/how-to-set-the-discovery-modes-for-multibranch-job-created-by-job-dsl)
* Suppress folder auto-triggering nuance (Stack Overflow) – [stackoverflow.com](https://stackoverflow.com/questions/77314303/dsl-script-suppressfolderautomatictriggering-property-isnt-working)

---

# GitHub Organizations – Key Limits & Guidance

## **Team Synchronization**

### ⚙️ Overview

The **Team Sync** feature enables automatic synchronization of your GitHub organization teams with identity provider (IdP) groups (e.g., Okta, Entra ID, Ping).

> ⚠️ **Important:** Exceeding usage limits may degrade performance or cause synchronization failures.

For details, see the official GitHub documentation:
👉 [Managing Team Synchronization for Your Organization](https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization)

---

### 📊 **Usage Limits**

| Resource                     | Maximum    | Notes                                            |
| ---------------------------- | ---------- | ------------------------------------------------ |
| Members per **team**         | **5,000**  | Large team syncs degrade performance.            |
| Members per **organization** | **10,000** | Total unique members allowed.                    |
| Teams per **organization**   | **1,500**  | Includes both manually created and synced teams. |

> 💡 **Tip:** Large synchronized groups should be avoided — sync only essential IdP groups required for access control.

---

### 🚀 **Best Practices & Optimization Tips**

* **Reuse existing teams** instead of creating per-repository “admin/write” teams.
  Use team-based permissions to manage repo access collectively.
* **Model hierarchy with nested teams**, rather than maintaining many flat teams.
  *(See: [About Teams](https://docs.github.com/en/organizations/organizing-members-into-teams/about-teams))*
* When syncing from **Entra ID** or **Okta**, select **only essential groups**.
  Synced teams count toward the 1,500-team limit.
* Regularly **review unused teams and memberships** to prevent sync bloat.
* If nearing caps, consider **manual team management** for smaller subgroups.

---

### 🧩 **Related GitHub Limits**

| Feature                      | Limit                   | Reference                                                                                                                                           |
| ---------------------------- | ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| GitHub Apps per organization | **100 registered apps** | [Registering a GitHub App](https://docs.github.com/en/apps/creating-github-apps/registering-a-github-app/registering-a-github-app)                  |
| GitHub App installations     | **Unlimited**           | [Installing a GitHub App](https://docs.github.com/en/apps/using-github-apps/installing-a-github-app-from-github-marketplace-for-your-organizations) |
| Repository limits            | *Varies by plan*        | [Repository Limits](https://docs.github.com/en/repositories/creating-and-managing-repositories/repository-limits)                                   |


Refs consolidated in **Quick Links → GitHub**.


---

## Multibranch Lifecycle Hygiene

* **Scanning:** prefer **webhooks**; keep periodic scans **low (e.g., daily)** for discovery only. *(See: [Webhooks & triggers](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin#_using_build_triggers_and_webhooks), [GitHub App authentication](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-app-auth)).*
* **Indexing/build storms:** enable **Initial Index Build Prevention** (CloudBees Build Strategies) to avoid mass first-run builds. *(See: [Build Strategies](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/cloudbees-build-strategies-plugin), [Prevent initial index builds (KB)](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/troubleshooting-guides/prevent-multibranch-organization-initial-index-builds), [Weathering build storms](https://www.cloudbees.com/blog/weathering-build-storms-in-the-enterprise)).*
* **Cloning:** avoid **submodules**; avoid custom clones. When history depth isn’t needed, use **shallow clones**. Consider a **Git reference repo** for large monorepos. *(See: [Git reference repository (KB)](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/client-and-managed-controllers/how-to-create-and-use-a-git-reference-repository)).*

---

## Recommended Defaults (Most Teams)

**Branch discovery**

* Strategy: **Exclude branches that are also filed as PRs** → prevents duplicate builds. *(See: [GitHub Branch Source docs](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin)).*

**PRs from origin**

* Strategy: **Build the PR merge with target** (integration signal). *(See: [GitHub Branch Source docs](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin), [Change Request strategy Javadoc](https://javadoc.jenkins.io/plugin/basic-branch-build-strategies/jenkins/branch/buildstrategies/basic/ChangeRequestBuildStrategyImpl.html)).*

**PRs from forks**

* Strategy: **Build PR merge**; Trust policy: **TrustPermission** (write/admin). Untrusted forks build with the **trusted Jenkinsfile** from target branch. *(See: [Fork PR trust policy Javadoc](https://javadoc.jenkins.io/plugin/github-branch-source/org/jenkinsci/plugins/github_branch_source/ForkPullRequestDiscoveryTrait.GitHubForkTrustPolicy.html), [Security & default permissions strategies](https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies)).*

**Initial indexing**

* Turn on **Initial Index Build Prevention** to create jobs without triggering builds. *(See: [Build Strategies](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/cloudbees-build-strategies-plugin), [Prevent initial index builds (KB)](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/troubleshooting-guides/prevent-multibranch-organization-initial-index-builds)).*

**Webhooks & API health**

* Use **GitHub webhooks**; reduce SCM polling. *(See: [Webhooks & triggers](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin#_using_build_triggers_and_webhooks)).*
* In **Manage Jenkins → System → GitHub**, enable **Normalize API requests / Throttle near rate limit**. *(See: [GitHub Branch Source docs](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin)).*

---

## Settings to Consider (Case-by-Case)

* **Build merge + head:** only if you need two signals (e.g., static analysis on head + integration on merge). *(See: [GitHub Branch Source docs](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin), [Change Request strategy Javadoc](https://javadoc.jenkins.io/plugin/basic-branch-build-strategies/jenkins/branch/buildstrategies/basic/ChangeRequestBuildStrategyImpl.html)).*
* **Name/regex filters:** include only `main`, `release/*`, meaningful `feature/*`. *(See: [Multibranch template syntax (GitHub)](https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/github)).*
* **Tags:** don’t auto-build all tags; add explicit tag filters or a release job. *(See: [Tag strategy Javadoc](https://javadoc.jenkins.io/plugin/basic-branch-build-strategies/jenkins/branch/buildstrategies/basic/TagBuildStrategyImpl.html)).*

---

## Settings to Avoid (In Most Orgs)

* **All branches + PR merge/head** together → duplicate builds. *(See: [GitHub Branch Source docs](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin)).*
* **Fork PR trust = Everyone** → untrusted Jenkinsfiles run with secrets. Use **TrustPermission**. *(See: [Fork PR trust policy Javadoc](https://javadoc.jenkins.io/plugin/github-branch-source/org/jenkinsci/plugins/github_branch_source/ForkPullRequestDiscoveryTrait.GitHubForkTrustPolicy.html), [Security & default permissions strategies](https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies)).*
* **Build everything on first indexing** → build storm. *(See: [Build Strategies](https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/cloudbees-build-strategies-plugin), [Prevent initial index builds (KB)](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/troubleshooting-guides/prevent-multibranch-organization-initial-index-builds)).*

---

## Git Hygiene & Performance

* Don’t use **both**`wipeWorkspaceTrait` and `cleanBeforeCheckout`. *(See: [Multibranch template syntax](https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/examples), [Basic Branch Build Strategies (plugin site)](https://docs.cloudbees.com/plugins/ci/basic-branch-build-strategies)).*
* Prefer **shallow clones** when possible; keep **LFS** only where needed. *(See: [Multibranch template syntax](https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/examples), [Git reference repository (KB)](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/client-and-managed-controllers/how-to-create-and-use-a-git-reference-repository)).*
* Use regex/wildcard filters to limit job creation. *(See: [Multibranch template syntax](https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/examples)).*

### GitHub Checks API

* SCM Reporting Plugin
* Checks API step
---

### Shallow clones

* Use shallow clones when possible

Full clone sample
```
Cloning into 'full_clone'...
remote: Enumerating objects: 3345151, done.
remote: Counting objects: 100% (3931/3931), done.
remote: Compressing objects: 100% (1119/1119), done.
remote: Total 3345151 (delta 3210), reused 3411 (delta 2798), pack-reused 3341220
Receiving objects: 100% (3345151/3345151), 1.06 GiB | 10.20 MiB/s, done.
Resolving deltas: 100% (2333278/2333278), done.
Updating files: 100% (79285/79285), done.
 Full clone took 139 seconds and used 2.3G of disk space.
```
Shallow clone sample
```
Starting shallow clone...
Cloning into 'shallow_clone'...
remote: Enumerating objects: 82748, done.
remote: Counting objects: 100% (82748/82748), done.
remote: Compressing objects: 100% (69641/69641), done.
remote: Total 82748 (delta 18138), reused 43648 (delta 12482), pack-reused 0
Receiving objects: 100% (82748/82748), 353.61 MiB | 8.33 MiB/s, done.
Resolving deltas: 100% (18138/18138), done.
Updating files: 100% (79285/79285), done.
 Shallow clone took 67 seconds and used 1.5G of disk space.
```
## Git hygiene & performance

* Don’t use **both** `wipeWorkspaceTrait` **and** `cleanBeforeCheckout`; pick one (usually `cleanBeforeCheckout`).
* Consider **shallow clones** (`shallow: true`, `depth: 50`) unless your build needs deep history, submodule exact SHAs, or `git describe`.
* Keep **LFS** only where repos actually use LFS (it slows fetches).
* Your **regex filter** is fine; for maintainability you can switch to “wildcards include/exclude” if the patterns allow. Note regex applies to *all heads* including PRs. ([CloudBees Docs][8])

---

# Focused JCasC Edits (diff-style)

```
items:
  - kind: organizationFolder
    name: <org-folder>

    buildStrategies:
      - initialIndexing:
          enabled: true                # avoid build storms
      - buildChangeRequests:
          ignoreUntrustedChanges: false
          ignoreTargetOnlyChanges: true
      - buildRegularBranches: {}
      # - buildTags: {}                # consider removing or restrict patterns

    navigators:
    - github:
        apiUri: https://api.github.com
        repoOwner: <org>
        credentialsId: <github-app-creds>
        traits:
          - gitHubBranchDiscovery:     # avoid duplicates
              strategyId: 1            # exclude branches that have PRs
          - gitHubPullRequestDiscovery:
              strategyId: 1            # build PR merge (1). Use 3 for merge+head.
          - gitHubForkDiscovery:
              strategyId: 1
              trust:
                $class: "org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustPermission"
          - cleanBeforeCheckout:
              extension:
                cleanBeforeCheckout:
                  deleteUntrackedNestedRepositories: true
          # - wipeWorkspaceTrait: {}    # avoid combining with cleanBeforeCheckout
          # - cloneOption:
          #     extension:
          #       cloneOption:
          #         shallow: true
          #         depth: 50
          #         noTags: false
          #         timeout: 30

    strategy:
      allBranchesSame:
        props:
          - suppressAutomaticTriggering:
              strategy: INDEXING        # suppress builds on indexing
              triggeredBranchesRegex: '.*'
```

---

## Observability & Troubleshooting

**Loggers (CasC):**

```
jenkins:
  logging:
    loggers:
      - name: "org.jenkinsci.plugins.github_branch_source"
        level: "FINEST"
      - name: "jenkins.scm.api"
        level: "FINEST"
      - name: "org.kohsuke.github"
        level: "FINEST"
      - name: "hudson.plugins.git"
        level: "FINEST"
      - name: "org.jenkinsci.plugins.gitclient"
        level: "FINEST"
      - name: "okhttp3"
        level: "FINEST"
      - name: "okhttp3.internal.http2"
        level: "FINEST"
```

**Proxy tail (example):**

```
kubectl -n squid exec -ti <squid-pod> -- \
  tail -f /var/log/squid/access.log | grep github
```

**What you’ll see:** REST/GraphQL calls, pagination, rate-limit, retries, and trait decisions; proxy shows timings and response codes for correlation.

---

## Typical Issues to Watch

* Submodule usage and deep history slowing clones.
* Aggressive scan intervals; missing webhook coverage.
* Non-optimal branch strategies (duplicate builds).
* Orphan branches/repos left behind—**prune** regularly.
* Shared library caching/shallow clone not enabled.
* Custom git steps that bypass traits (inefficient).
* Caches are not enabled

---

## TL;DR

* **PR merge builds**, **trust forks by permission**, and **Initial Index Build Prevention** are the big wins.
* Flip **`ignoreTargetOnlyChanges: true`** to cut noisy PR rebuilds.
* Be intentional with **tags**, **clone depth**, and **workspace cleanup**.
* Put **trigger suppression** on the **branch property** (not folder) so it applies to children.
