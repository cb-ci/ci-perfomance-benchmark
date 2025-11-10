#! /bin/bash

source ./set-env.sh
REPO="$GIT_REPO_OWNER/$GIT_REPO_NAME"
BASE_SHA=$(gh api repos/$REPO/git/ref/heads/$DEFAULT_BRANCH --jq '.object.sha')

# Loop to create 20 branches from main
for i in $(seq 1 20); do
  gh api repos/$REPO/git/refs \
    -X POST \
    -F ref="refs/heads/feature-branch-$i" \
    -F sha="$BASE_SHA" 2>&1 > /dev/null
done
