# GitHub API Rate Limiting PoC Scripts

This directory contains a set of scripts to manage repository, branches and PRs within your GitHub Organistion.
The scripts create and delete repositories, branches, and pull requests.
The scripts can be used and eextended/adjusted for testing purposes. 

See  
* https://cli.github.com/manual/gh_api
* https://cli.github.com/manual/gh

## Scripts

### Repository Management

*   **`gh-repoCreate.sh`**: Creates 10 repositories from a template. The new repositories are named `${GIT_REPO_NAME}-1` to `${GIT_REPO_NAME}-X`.
*   **`gh-repoDelete.sh`**: Deletes the 10 repositories created by `gh-repoCreate.sh`.

### Branch Management

*   **`gh-branchCreate.sh`**: Creates 20 branches named `feature-branch-1` to `feature-branch-20`. The branches are created from the `main` branch of the repository.
*   **`gh-branchDelete.sh`**: Deletes all branches in the repository except for the default branch.

### Pull Request Management

*   **`gh-prCreate.sh`**: Creates a pull request for each of the 20 feature branches. The pull requests are created against the `main` branch.
*   **`gh-prDelete.sh`**: Closes all open pull requests in the repository.

## Usage

1.  **Configure Environment Variables:**
    *   Copy the `set-env-template.sh` file to `set-env.sh`.
        ```bash
        cp set-env-template.sh set-env.sh
        ```
    *   Edit `set-env.sh` and fill in the required environment variables:
        *   `GIT_REPO_OWNER`: Your GitHub username or organization.
        *   `GIT_REPO_NAME`: The name of the repository.
        *   `GIT_REPO_TEMPLATE`: The template repository to use.
        *   `TEAM_SLUG`: The slug of the team to grant permissions.
        *   `DEFAULT_BRANCH`: The default branch of the repository.

2.  **Run the scripts:**
    *   Make sure the scripts are executable:
        ```bash
        chmod +x *.sh
        ```
    *   Run the desired script:
        ```bash
        ./gh-repoCreate.sh
        ```