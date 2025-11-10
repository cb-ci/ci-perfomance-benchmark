#! /bin/bash

source ./set-env.sh
# REPO="$GIT_REPO_OWNER/$GIT_REPO_NAME"

git fetch
# List all open PRs, then close each
for pr in $(gh pr list --state open --json number -q '.[].number'); do
  echo "Closing PR #$pr"
  gh pr close $pr
done
