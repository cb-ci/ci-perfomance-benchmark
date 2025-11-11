#!/bin/bash

# This script sends a Groovy script to the Jenkins /scriptText endpoint.

# Usage: ./send_groovy_to_jenkins.sh <JENKINS_HOST>  <JENKINS_API_TOKEN>

source ../set-env.sh

GROOVY_SCRIPT_PATH="$(pwd)/filePerfIOMetrics.groovy"
#GROOVY_SCRIPT_PATH="$(pwd)/testEnv.groovy"

if [ -z "$JENKINS_HOST" ] || [ -z "$JENKINS_API_TOKEN" ]; then
    echo "Usage: $0 <JENKINS_HOST>  <JENKINS_API_TOKEN> "
    exit 1
fi

if [ ! -f "$GROOVY_SCRIPT_PATH" ]; then
    echo "Error: Groovy script file not found at '$GROOVY_SCRIPT_PATH'"
    exit 1
fi

echo "Sending Groovy script '$GROOVY_SCRIPT_PATH' to Jenkins at '$JENKINS_HOST/scriptText'..."

# Read the Groovy script content
GROOVY_SCRIPT_CONTENT=$(cat "$GROOVY_SCRIPT_PATH")

# Send the script to Jenkins using curl
curl -o fio-metics.txt -s -X POST \
     -u "${JENKINS_API_TOKEN}" \
     "${JENKINS_HOST}/scriptText" \
     --data-urlencode "script=${GROOVY_SCRIPT_CONTENT}" \
     --data-urlencode "json={}" \
     -H "Content-Type: application/x-www-form-urlencoded"

echo ""
echo "Script sent. Check Jenkins console for output."
cat fio-metics.txt
