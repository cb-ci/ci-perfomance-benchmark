# GitHub Branch Source: Recommended Setup & Pitfalls (CloudBees CI)

Use this as a field-ready checklist for configuring **Multibranch & Organization** projects with **GitHub Branch Source** (CloudBees CI/Jenkins).

## Table of Contents

* [Quick Links](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#quick-links)
* [GitHub Organizations – Key Limits & Guidance](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#github-organizations--key-limits--guidance)
* [Multibranch Lifecycle Hygiene](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#multibranch-lifecycle-hygiene)
* [Recommended Defaults (Most Teams)](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#recommended-defaults-most-teams)
* [Settings to Consider (Case-by-Case)](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#settings-to-consider-case-by-case)
* [Settings to Avoid (In Most Orgs)](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#settings-to-avoid-in-most-orgs)
* [Git Hygiene & Performance](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#git-hygiene--performance)
* [Focused JCasC Edits (diff-style)](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#focused-jcasc-edits-diff-style)
* [Observability & Troubleshooting](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#observability--troubleshooting)
* [Typical Issues to Watch](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#typical-issues-to-watch)
* [TL;DR](https://chatgpt.com/c/68ed0543-7b58-8333-9af9-e8615167279e#tldr)

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

## GitHub Organizations – Key Limits & Guidance

* **Teams:** up to **1,500** per org; **members:** up to **10,000**; **team size:** up to **5,000**. Prefer **nested teams**; avoid per-repo teams. *(See: [Team sync usage limits](https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization#usage-limits), [About teams](https://docs.github.com/en/organizations/organizing-members-into-teams/about-teams), [Repository limits](https://docs.github.com/en/repositories/creating-and-managing-repositories/repository-limits)).*
* **GitHub Apps:** up to **100 apps registered** per org; **installations are unlimited**. *(See: [Registering a GitHub App](https://docs.github.com/en/apps/creating-github-apps/registering-a-github-app/registering-a-github-app), [Installing a GitHub App](https://docs.github.com/en/apps/using-github-apps/installing-a-github-app-from-github-marketplace-for-your-organizations)).*
* **Team sync:** large syncs degrade performance—sync only essential IdP groups. *(See: [SAML / team sync](https://docs.github.com/en/enterprise-cloud@latest/organizations/managing-saml-single-sign-on-for-your-organization/managing-team-synchronization-for-your-organization)).*

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

---

## Focused JCasC Edits (diff-style)

```yaml
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
              strategy: INDEXING        # suppress builds during indexing
              triggeredBranchesRegex: '.*'
```

---

## Observability & Troubleshooting

**Loggers (CasC):**

```yaml
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

```bash
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

---

## TL;DR

* **PR merge builds**, **trust forks by permission**, and **Initial Index Build Prevention** are the big wins.
* Flip **`ignoreTargetOnlyChanges: true`** to cut noisy PR rebuilds.
* Be intentional with **tags**, **clone depth**, and **workspace cleanup**.
* Put **trigger suppression** on the **branch property** (not folder) so it applies to children.
