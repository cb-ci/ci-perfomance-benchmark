#! /bin/bash

set -x

source ./set-env.sh

#gh auth login

# Creates X repositories from teamplate in a existing GH Organisation
# Adusjt how many: {1..10}
for repo  in {1..10}; do
  TMP_REPO_NAME="${GIT_REPO_NAME}-${repo}"
  echo "Create repo $TMP_REPO_NAME from template $REPO_TEMPLATE"
  # Create inside an org and include ALL branches from the template
  gh repo create $GIT_REPO_OWNER/$TMP_REPO_NAME \
    --template $GIT_REPO_OWNER/$GIT_REPO_TEMPLATE \
    --description "Test Repo $TMP_REPO_NAME from template $REPO_TEMPLATE" \
    --include-all-branches --public
     #     --team team1 \
  # Assign team to repo for TEAM_SLUGS
  gh api \
    -X PUT \
    "/orgs/$GIT_REPO_OWNER/teams/$TEAM_SLUG/repos/$GIT_REPO_OWNER/$TMP_REPO_NAME" \
    -f permission="admin"  2>&1 > /dev/null    # pull | triage | push | maintain | admin
done




