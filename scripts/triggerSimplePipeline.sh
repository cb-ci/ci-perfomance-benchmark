#! /bin/bash

#source ../set-env.sh
#TOKEN="$JENKINS_USERNAME:$JENKINS_API_TOKEN"
#curl -u "$JENKINS_API_TOKEN" -X POST "$JENKINS_HOST/$JENKINS_JOB_PATH"


JENKINS=https://jenkins.example
USER=svc
TOKEN=xxx
JOB='perf/LoadJob'

#CRUMB=$(curl -s -u "$USER:$TOKEN" "$JENKINS/crumbIssuer/api/json" | jq -r .crumb)
#curl -H "Jenkins-Crumb:$CRUMB" ....

for i in {1..10}; do
  curl -s -u "$USER:$TOKEN" \
    -X POST "$JENKINS/job/$JOB/buildWithParameters?PROFILE=medium&cause=perf"
done