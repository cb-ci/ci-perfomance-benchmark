import os
from locust import HttpUser, task, between

# --- Configuration for Jenkins Authentication ---
# IMPORTANT: In a real-world scenario, you MUST provide credentials to trigger a Jenkins build.
# Set these environment variables before running Locust:
# export JENKINS_USERNAME="your_username"
# export JENKINS_API_TOKEN="your_api_token"

JENKINS_API_TOKEN = os.environ.get("JENKINS_API_TOKEN", "jenkins_token")
JENKINS_HOST = os.environ.get("JENKINS_HOST", "http://localhost:8080")
JENKINS_JOB_PATH = os.environ.get("JENKINS_JOB_PATH", "/job/test-triggers/job/simple/build")
#JENKINS_JOB_PATH = os.environ.get("JENKINS_JOB_PATH", "/job/test-triggers/job/root/build")

class JenkinsPipelineUser(HttpUser):
    # The base host of your Jenkins server.
    # The path will be appended to this host for requests.
    host = (JENKINS_HOST)

    # Wait time between tasks: wait between 1 and 2 seconds
    wait_time = between(1, 2)

    def on_start(self):
        """Called when a Locust user is spawned"""
        print(f"Starting Locust user for Jenkins host: {self.host}")

        # Set up Basic Authentication header for all requests.
        # This is typically required by Jenkins for build triggering.
        self.auth = (JENKINS_API_TOKEN)

    @task
    def trigger_pipeline_build(self):
        """
        Triggers an unparameterized build of the 'test-triggers/root' pipeline.

        We use POST to the '/build' endpoint, which is the standard way to trigger
        a Jenkins job, especially when authentication is required.
        """
        # The path corresponds to the provided URL:
        # /casc-pipeline-templates/job/test-triggers/job/root/build

        build_path = (JENKINS_JOB_PATH)

        # Note: Jenkins typically expects a POST for builds.
        # The auth tuple is automatically used by the client.
        response = self.client.post(
            build_path,
            name="/build-pipeline", # A friendly name for reporting/grouping requests
            auth=self.auth
        )

        # Basic status check (Jenkins usually returns 201 Created or 302 Redirect
        # for a successful, unparameterized build trigger).
        if response.status_code not in [201, 302]:
            print(f"Failed to trigger build. Status: {response.status_code}")

        # If your pipeline has parameters, the path changes to /buildWithParameters
        # and you would include data:
        # self.client.post(
        #     "/casc-pipeline-templates/job/test-triggers/job/root/buildWithParameters",
        #     data={"PARAM_NAME": "value"},
        #     auth=self.auth
        # )