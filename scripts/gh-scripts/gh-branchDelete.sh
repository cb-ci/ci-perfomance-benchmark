#! /bin/bash

source ./set-env.sh
REPO="$GIT_REPO_OWNER/$GIT_REPO_NAME"

# List all branches, exclude default, then delete each
for br in $(gh api repos/$REPO/branches --paginate --jq '.[].name' | grep -v "^$DEFAULT_BRANCH$"); do
  echo "Deleting branch $br"
  gh api repos/$REPO/git/refs/heads/$br -X DELETE 2>&1 > /dev/null
done