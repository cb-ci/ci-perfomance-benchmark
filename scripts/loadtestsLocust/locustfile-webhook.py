# locustfile.py
from locust import HttpUser, task, between
import random, json, time

class WebhookUser(HttpUser):
    wait_time = between(0.05, 0.2)
    @task
    def github_webhook(self):
        payload = {
          "zen":"Keep it logically awesome",
          "repository":{"full_name":f"perf/repo-{random.randint(1,2000)}"},
          "pull_request":{"number":random.randint(1,50000)},
          "action": random.choice(["opened","synchronize","reopened"])
        }
        headers={"X-GitHub-Event":"pull_request","Content-Type":"application/json"}
        self.client.post("/github-webhook/", data=json.dumps(payload), headers=headers, name="github-pr")