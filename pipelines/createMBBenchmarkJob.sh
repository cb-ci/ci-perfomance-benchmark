#! /biun/bash

# Required variables:

# Controller URL
export JENKINS_HOST="https://ci.example.org"
export JENKINS_API_TOKEN="USER:XXX"
export JENKINS_HOST="https://dev.sda.acaternberg.flow-training.beescloud.com/casc-pipeline-templates/"
export JENKINS_API_TOKEN="admin:11bdda92f92b9ede621141b39653603d5e"


# JobName
export JOB_NAME="TEST_JOB_1"
# The GIT Organisation name where the Application repository is located
export GIT_REPO_OWNER="MY-GITHUB_ORG"

# The GIT repo name (Your App reponame)
export GIT_REPO="ci-perfomance-benchmarks"

# The credentials id of your GitHub App credentials
#see https://docs.cloudbees.com/docs/cloudbees-ci/latest/traditional-admin-guide/github-app-auth
export GIT_HUP_APP_CREDENTIAL_ID="ci-gh-app"

envsubst < items-MBBenchmark.yaml > MB-${JOB_NAME}.yaml
cat MB-${JOB_NAME}.yaml
echo "------------------  CREATING MANAGED CONTROLLER ------------------"
curl -v -XPOST \
   --user $JENKINS_API_TOKEN \
   "${JENKINS_HOST}/casc-items/create-items" \
    -H "Content-Type:text/yaml" \
   --data-binary @MB-${JOB_NAME}.yaml