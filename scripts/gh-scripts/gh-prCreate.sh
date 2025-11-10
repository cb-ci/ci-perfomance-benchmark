#! /bin/bash

source ./set-env.sh

git fetch


for i in $(seq 1 20); do
  BR="feature-branch-$i"
  # Ensure branch exists locally
  git checkout "$BR" || git checkout -b "$BR" origin/"$BR" 2>&1 > /dev/null

  # Create PR with title and body
  gh pr create \
    --base "$DEFAULT_BRANCH" \
    --head "$BR" \
    --title "PR for $BR" \
    --body "This PR introduces changes from $BR."  2>&1 > /dev/null
done
