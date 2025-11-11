# GitHub Branch Source Best Practices

This document outlines the recommended setup for **GitHub Branch Source** in Multibranch and Organization projects, along with common pitfalls to avoid.

## Links 

* https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/cloudbees-build-strategies-plugin
* https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-app-auth
* https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin#_using_build_triggers_and_webhooks
* https://github.com/jenkinsci/github-branch-source-plugin
* https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies
* https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization
* https://resources.github.com/learn/pathways/administration-governance/essentials/strategies-for-using-organizations-github-enterprise-cloud/
* https://docs.github.com/en/repositories/creating-and-managing-repositories/repository-limits
* https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-app-auth
* https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies

---

## GitHub Organizations

See https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies

### Teams

https://docs.github.com/en/organizations/organizing-members-into-teams/about-teams

### Number of GitHub Apps per Organization

* You can **register up to 100 GitHub Apps** owned by a single organization. ([GitHub Docs][1])
* There’s **no limit on how many GitHub Apps you can install** on an organization (installations are unlimited). ([GitHub Docs][2])

If you’re deciding between creating more org-owned apps vs. installing third-party apps, I can help pick the cleanest model (permissions, webhooks, rate limits, etc.).

See
[1]: https://docs.github.com/en/apps/creating-github-apps/registering-a-github-app/registering-a-github-app "Registering a GitHub App"
[2]: https://docs.github.com/en/apps/using-github-apps/installing-a-github-app-from-github-marketplace-for-your-organizations "Installing a GitHub App from GitHub Marketplace for your ..."

### Number of repositories, teams and users

There are usage limits for the team synchronization feature. Exceeding these limits will lead to a degradation in performance and may cause synchronization failures.
* Maximum number of members in a GitHub team: 5,000
* Maximum number of members in a GitHub organization: 10,000
* Maximum number of teams in a GitHub organization: 1,500
* See https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization#usage-limits


**1,500 teams per organization.**
GitHub’s docs note a cap of **1,500 teams** in a single org (mentioned in their team-sync limits, but it applies to orgs generally). ([GitHub Docs][1])

Tips if you’re nearing the cap:

* Avoid creating per-repo “admin/write” teams; reuse broader teams and grant repo access via team permissions.
* Use **nested teams** to model hierarchy instead of many flat teams. ([GitHub Docs][2])
* If you sync from Entra ID/Okta, select only essential groups—synced teams still count toward the 1,500. ([GitHub Docs][1])

[1]: https://docs.github.com/enterprise-cloud%40latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization "Managing team synchronization for your organization"
[2]: https://docs.github.com/organizations/organizing-members-into-teams/about-teams "About organization teams"

---

## Multibranch Lifecycle

* **Scan interval:** Configure appropriate scan intervals and filters.
* **Indexing:**
    * https://www.cloudbees.com/blog/weathering-build-storms-in-the-enterprise
    * https://docs.cloudbees.com/docs/release-notes/latest/plugins/cloudbees-build-strategies-plugin/
* **PR:** Define a clear PR strategy.
* **Clone:**
    * Avoid sub-modules.
    * Avoid custom clones.
    * Use a git-ref-repo: https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/client-and-managed-controllers/how-to-create-and-use-a-git-reference-repository

---

## MultiBranch Source Recommended defaults (most teams)

### Branch discovery

* **Strategy:** *Exclude branches that are also filed as PRs.*
  Prevents duplicate builds when the same changes appear in both a branch job and the PR job. ([docs.cloudbees.com][1])

### PRs from the origin repo

* **Strategy:** *Build the PR **merge** with the target branch.*
  Catches integration conflicts early; this is the usual CI signal you want. ([docs.cloudbees.com][1])

### PRs from forks

* **Strategy:** *Build the PR **merge*** **and**
* **Trust policy:** *Only trust users with **write/admin** permission* (`TrustPermission`).
  This runs the Pipeline with the PR’s proposed code **only** when the author is a trusted collaborator; otherwise Jenkins uses the trusted Jenkinsfile from the target branch (safer). ([docs.cloudbees.com][1])

### Initial indexing / “build storm” protection

* Enable **Initial Index Build Prevention** (CloudBees Build Strategies). It creates jobs but **does not** build everything during first scan; builds start on the next change. Huge win for large orgs. ([docs.cloudbees.com][2])
* Use the [CloudBees-build-strategies-plugin](https://docs.cloudbees.com/docs/release-notes/latest/plugins/cloudbees-build-strategies-plugin/)
* See
    * https://www.cloudbees.com/blog/weathering-build-storms-in-the-enterprise
    * https://docs.cloudbees.com/docs/release-notes/latest/plugins/cloudbees-build-strategies-plugin/

### Webhooks & scans

* Use **GitHub webhooks** for fast PR/branch events; keep **periodic scans** low (e.g., daily) to discover new repos/branches. ([docs.cloudbees.com][3])
* Avoid SCM Pooling, use webhooks
* https://github.com/cb-ci/ci-groovy/blob/main/printAllSCMPollings.groovy

### GitHub API health

* In **Manage Jenkins → System → GitHub**, set **Normalize API requests** (or “Throttle at/near rate limit”) to avoid rate-limit stalls when you have many multibranch jobs. ([docs.cloudbees.com][3])
* Setup guide and advanced permissions
    * https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-app-auth
    * https://github.com/jenkinsci/github-branch-source-plugin/blob/master/docs/github-app.adoc#enhancing-security-using-repository-access-strategies-and-default-permissions-strategies

---

## Settings to consider (case-by-case)

* **Build PR “merge + head” (both strategies):** use when you need *two* signals (e.g., static analysis on PR head + integration test on merge). Expect ~2× build cost. Default to **merge only** otherwise. ([docs.cloudbees.com][1])
* **Filter by name/regex:** include only `main`, `release/*`, and meaningful `feature/*` to reduce noise and API traffic. ([docs.cloudbees.com][3])
* **Tags:** avoid auto-discovering/building **all tags**; if needed, add an explicit tag filter or a release job. (General multibranch guidance.) ([docs.cloudbees.com][3])
* **Submodules** Avoid submodules when possible **all tags**; if needed, add an explicit tag filter or a release job. (General multibranch guidance.) ([docs.cloudbees.com][3])

### GitHub Checks API

* SCM Reporting Plugin
* Checks API step

---

## Settings to avoid (in most orgs)

* **Branch discovery = “All branches”** **and** **PR discovery (merge/head)** together → doubles builds for the same changes. Prefer *Exclude branches that are also filed as PRs*. ([docs.cloudbees.com][1])
* **Fork PR trust = “Everyone”** → lets arbitrary contributors run unreviewed Jenkinsfiles with your credentials. Use **TrustPermission** (write/admin) and let untrusted forks use the target branch’s Jenkinsfile. ([docs.cloudbees.com][1])
* **Building everything on first indexing** in large orgs → classic “build storm”. Use the **Initial Index Build Prevention** strategy. ([docs.cloudbees.com][2])

### Submodules

* Avoid submodules when possible

### Shallow clones

* Use shallow clones when possible


 
 
Sample run :
 
Cloning into 'full_clone'...
remote: Enumerating objects: 3345151, done.
remote: Counting objects: 100% (3931/3931), done.
remote: Compressing objects: 100% (1119/1119), done.
remote: Total 3345151 (delta 3210), reused 3411 (delta 2798), pack-reused 3341220
Receiving objects: 100% (3345151/3345151), 1.06 GiB | 10.20 MiB/s, done.
Resolving deltas: 100% (2333278/2333278), done.
Updating files: 100% (79285/79285), done.
 Full clone took 139 seconds and used 2.3G of disk space.


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
`

---

## Why these choices work

* They give you **one integration-grade signal per PR** (merge build) without duplicate branch builds.
* They **contain risk** from untrusted forks while keeping a smooth contributor experience.
* They **protect controllers** during first adoption and keep **GitHub API** usage predictable. ([docs.cloudbees.com][3])

---

## What’s good already

* **Branch discovery = `strategyId: 1`** (exclude branches that also have PRs) → avoids duplicate builds. ([Stack Overflow][1])
* **Weekly folder scan** and **orphan pruning** → gentler on the API & disk.

---

## Tighten the PR/branch strategies

1. **Build PRs as the *merge* with target**
   Your PR discovery is `strategyId: 2` (PR **head**). Switch to **1** (PR **merge**) so you test what would actually land on `main`. Optionally use **3** to build both if you need two signals. ([CloudBees Docs][2])
2. **Fork PR trust policy: use “permission”**
   You currently use `gitHubTrustContributors`. Prefer **`TrustPermission`** (write/admin) to keep secrets safe; untrusted forks still build using the *trusted* Jenkinsfile from the target branch. ([Jenkins Javadoc][3])
3. **Prevent build storms on first index**
   Add **Initial Index Build Prevention**. It creates jobs but doesn’t run them until the next change—critical for big orgs. ([CloudBees Docs][4])
4. **Change Request strategy**
   Set `ignoreTargetOnlyChanges: true` so PRs don’t rebuild just because the target branch moved. Keep `ignoreUntrustedChanges: false` so untrusted forks still get built (with the trusted Jenkinsfile). ([Jenkins Javadoc][5])
5. **Tags: be deliberate**
   Building tags is fine if you actually release from tags. Otherwise, remove the tag strategy (it adds noise and API work). If you keep it, limit the window and/or patterns (e.g., only `v*`). ([Jenkins Javadoc][6])
6. **Suppress automatic SCM triggering: move it to the branch property**
   The folder-level `suppressFolderAutomaticTriggering` often doesn’t affect multibranch children. Use **`strategy → allBranchesSame → props → suppressAutomaticTriggering`** instead. ([Stack Overflow][7])

---

## Git hygiene & performance

* Don’t use **both** `wipeWorkspaceTrait` **and** `cleanBeforeCheckout`; pick one (usually `cleanBeforeCheckout`).
* Consider **shallow clones** (`shallow: true`, `depth: 50`) unless your build needs deep history, submodule exact SHAs, or `git describe`.
* Keep **LFS** only where repos actually use LFS (it slows fetches).
* Your **regex filter** is fine; for maintainability you can switch to “wildcards include/exclude” if the patterns allow. Note regex applies to *all heads* including PRs. ([CloudBees Docs][8])

---

## Focused JCasC edits (only the parts to change)

```
items:
  - kind: organizationFolder
    name: mecs

    # 1) Build strategies
    buildStrategies:
      - initialIndexing:            # add this
          enabled: true
      - buildChangeRequests:
          ignoreUntrustedChanges: false
          ignoreTargetOnlyChanges: true   # was false
      - buildRegularBranches: {}
      # - buildTags:                 # consider removing entirely...
      #     atLeastDays: '0'         # ...or keep with tight bounds/patterns
      #     atMostDays: '7'

    navigators:
    - github:
        apiUri: https://api.github.com
        repoOwner: cb-ci-templates
        credentialsId: ci-template-gh-app
        traits:
          # Branch discovery: keep 1 (exclude branches that also have PRs)
          - gitHubBranchDiscovery:
              strategyId: 1

          # PRs from origin: switch to MERGE (1). Use 3 if you also need HEAD.
          - gitHubPullRequestDiscovery:
              strategyId: 1           # was 2

          # PRs from forks: MERGE + trust by permission (write/admin)
          - gitHubForkDiscovery:
              strategyId: 1
              trust:
                $class: "org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustPermission"

          # (Optional) shallow clone to cut network IO—only if builds don’t need deep history
          # - cloneOption:
          #     extension:
          #       cloneOption:
          #         shallow: true
          #         depth: 50
          #         noTags: false
          #         timeout: 30

          # Prefer ONE of the following two:
          - cleanBeforeCheckout:
              extension:
                cleanBeforeCheckout:
                  deleteUntrackedNestedRepositories: true
          # - wipeWorkspaceTrait: {}   # remove if using cleanBeforeCheckout

          # keep your existing: headRegexFilter, statusChecks, submoduleOption, lfs, checkoutOption...
          - headRegexFilter:
              regex: ^bifrost-devhelp-code.*|^master$|^main$|^dev$|^ufe_dev$|^[\d\-\.]*release$|^release\/\d+(\.\d+)?$|PR-[0-9]+|[\d\-\.]+(_Update)?|^release\/\S+\/\d+(\.\d+)*?$|^(main|release)\/v\d+$

    # 2) Put “suppress automatic triggering” on the branch property, not folder
    strategy:
      allBranchesSame:
        props:
          - suppressAutomaticTriggering:
              strategy: INDEXING        # suppress builds on indexing
              triggeredBranchesRegex: '.*'

    # Remove duplicate/ineffective folder-level suppressors:
    # properties:
    #   - suppressFolderAutomaticTriggering: { ... }   # delete both occurrences
```

---

## Additional Resources

* Custom git steps
* MB Build strategy extensions
* https://docs.cloudbees.com/plugins/ci/basic-branch-build-strategies
* https://plugins.jenkins.io/basic-branch-build-strategies/
* https://docs.cloudbees.com/plugins/ci/multibranch-build-strategy-extension

---

## Loggers

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
````

---

## Proxy logs

`
kubectl exec -ti squid-dev-proxy-5bf5f6fff4-kx42l -n squid   -- tail -f /var/log/squid/access.log |grep github
`

---

## What you’ll see

From loggers: repository/PR/branch discovery queries, GraphQL/REST endpoints hit, pagination, rate-limit remaining, backoffs/retries, and decisions about what jobs to create/update. (These come from github-branch-source, SCM API, and the GitHub API client.)
docs.cloudbees.com

From proxy: every request line & response code, with timestamps, and (if enabled) headers. This is perfect for correlating with GitHub’s own audit/rate-limit data.

---

## Typical Issues

* Potential Usage of Submodule References
* Aggressive scan intervals have been seen
* Additional non-optimal job configurations according to branch strategies
* Clean up orphan branches and repositories!
* Verify if shared library caching is enabled!
* Verify if shared Library shallow clone is enabled
* Set up sample projects a GH org for benchmarking  (GH repos and jobs at scale)
* approaches and design are founded allready
* Avoid custom git steps (or make them effcient)

---

## TL;DR

* **PR merge builds (1)**, **fork trust by permission**, and **Initial Index Build Prevention** are the big wins.
* Flip **`ignoreTargetOnlyChanges` → true** to cut noisy PR rebuilds.
* Be intentional with **tags** and **workspace cleanup**.
* Move **trigger suppression** into the **branch property** so it actually applies to children.

[1]: https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/examples
[2]: https://docs.cloudbees.com/docs/cloudbees-ci/latest/traditional-admin-guide/cloudbees-build-strategies-plugin
[3]: https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/github-branch-source-plugin
[1]: https://stackoverflow.com/questions/67871598/how-to-set-the-discovery-modes-for-multibranch-job-created-by-job-dsl?utm_source=chatgpt.com
[2]: https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/examples?utm_source=chatgpt.com
[3]: https://javadoc.jenkins.io/plugin/github-branch-source/org/jenkinsci/plugins/github_branch_source/ForkPullRequestDiscoveryTrait.GitHubForkTrustPolicy.html?utm_source=chatgpt.com
[4]: https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/troubleshooting-guides/prevent-multibranch-organization-initial-index-builds?utm_source=chatgpt.com
[5]: https://javadoc.jenkins.io/plugin/basic-branch-build-strategies/jenkins/branch/buildstrategies/basic/ChangeRequestBuildStrategyImpl.html?utm_source=chatgpt.com
[6]: https://javadoc.jenkins.io/plugin/basic-branch-build-strategies/jenkins/branch/buildstrategies/basic/TagBuildStrategyImpl.html?utm_source=chatgpt.com
[7]: https://stackoverflow.com/questions/77314303/dsl-script-suppressfolderautomatictriggering-property-isnt-working?utm_source=chatgpt.com
[8]: https://docs.cloudbees.com/docs/cloudbees-ci/latest/multibranch-pipeline-template-syntax-guide/github?utm_source=chatgpt.com
