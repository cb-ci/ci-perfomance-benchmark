#! /bin/bash

set -x

source ./set-env.sh

for repo in {1..10}; do
  TMP_REPO_NAME="${GIT_REPO_NAME}-${repo}"
  echo "Deleting repo $TMP_REPO_NAME"
  gh repo delete "$GIT_REPO_OWNER/$TMP_REPO_NAME" --yes
done
