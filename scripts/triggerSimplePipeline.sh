#! /bin/bash

source ./set-env.sh
#TOKEN="$JENKINS_USERNAME:$JENKINS_API_TOKEN"
#curl -u "$JENKINS_API_TOKEN" -X POST "$JENKINS_HOST/$JENKINS_JOB_PATH"




#CRUMB=$(curl -s -u "$USER:$TOKEN" "$JENKINS/crumbIssuer/api/json" | jq -r .crumb)
#curl -H "Jenkins-Crumb:$CRUMB" ....

for i in {1..5}; do
  echo "trigger build $i"
  curl -s -u "$JENKINS_API_TOKEN" \
    -X POST "$JENKINS_HOST/$JENKINS_JOB_PATH"
    #-X POST "$JENKINS/job/$JOB/buildWithParameters?PROFILE=medium&cause=perf"
    sleep 2
done