# Locust Performance Testing Scripts

This directory contains scripts for performance testing using [Locust](https://locust.io/).

## Table of Contents
- [Locust Overview](#locust-overview)
- [Installation and Setup](#installation-and-setup)
- [Locust Usage](#locust-usage)
- [Defining User Profiles (locustfile.py)](#defining-user-profiles-locustfilepy)
- [Useful Examples](#useful-examples)

## Locust Overview
Locust is an open-source load testing tool. It allows you to define user behavior with Python code and then swarm your system with millions of simultaneous users.

- **Official Website**: https://locust.io/
- **Locust Web UI**: Typically accessible at `http://0.0.0.0:8089/` after starting Locust.
- **Main Locustfile**: see [locustfile-triggerTestBuild.py](locustfile-triggerTestBuild.py)

## Installation and Setup

1.  **Install Locust**:
    ```bash
    pip install locust
    ```

2.  **Start Locust (with Web UI)**:
    ```bash
    locust -f locustfile.py
    open http://0.0.0.0:8089/
    ```
    This command starts Locust and opens the web interface in your browser, where you can configure and start the test.

## Locust Usage

Locust can be run in various modes:

### 1. With Web UI (Interactive Mode)
This is the most common way to run Locust for development and monitoring.

```bash
locust -f locustfile.py --host https://<your-target-host>
```
Then open `http://0.0.0.0:8089/` in your browser. You can specify the number of users and their spawn rate directly in the UI.

### 2. Headless Mode (Non-Interactive)
For automated tests (e.g., in CI/CD pipelines), you can run Locust without the web UI.

```bash
locust -f locustfile.py --host https://<your-target-host> -u 2000 -r 200 --run-time 10m --csv=results --headless
```
-   `-u 2000`: Simulate 2000 users.
-   `-r 200`: Spawn 200 users per second.
-   `--run-time 10m`: Run the test for 10 minutes.
-   `--csv=results`: Store statistics in CSV files prefixed with `results`.
-   `--headless`: Run without the web UI.

### 3. Distributed Testing (Master/Worker Setup)
For very high loads, you can distribute the test across multiple machines.

**On the Master Node:**
```bash
locust -f locustfile.py --master
```

**On Worker Nodes:**
```bash
locust -f locustfile.py --worker --master-host=<master-node-ip>
```

## Defining User Profiles (locustfile.py)

The `locustfile.py` (or any file specified with `-f`) defines the user behavior. Each `User` class represents a type of user or a specific scenario.

### Basic Structure:

```python
from locust import HttpUser, task, between

class WebsiteUser(HttpUser):
    wait_time = between(1, 2) # Users will wait between 1 and 2 seconds between tasks
    host = "http://localhost:8080" # Default host if not specified via CLI

    @task
    def view_homepage(self):
        self.client.get("/")

    @task(3) # This task has a weight of 3, meaning it's 3 times more likely to be executed
    def view_items(self):
        self.client.get("/items")

    @task
    def view_about(self):
        self.client.get("/about")
```

### User Profiles and Task Sets:

You can define different user types with varying behaviors and assign weights to them to simulate a realistic mix of users.

```python
from locust import HttpUser, TaskSet, task, between, constant

class AnonymousUser(TaskSet):
    @task
    def view_homepage(self):
        self.client.get("/")

    @task
    def view_about(self):
        self.client.get("/about")

class AuthenticatedUser(TaskSet):
    def on_start(self):
        self.login() # Perform login when user starts

    def login(self):
        self.client.post("/login", {"username":"test", "password":"password"})

    @task
    def view_dashboard(self):
        self.client.get("/dashboard")

    @task
    def post_data(self):
        self.client.post("/data", {"value": "some_data"})

class WebsiteUser(HttpUser):
    wait_time = constant(2) # Wait exactly 2 seconds between tasks
    host = "http://localhost:8080"

    tasks = {
        AnonymousUser: 7,  # 70% of users will be anonymous
        AuthenticatedUser: 3 # 30% of users will be authenticated
    }
```

## Useful Examples

### 1. Simulating a User Journey (Login -> Browse -> Logout)

```python
from locust import HttpUser, task, between

class UserJourney(HttpUser):
    wait_time = between(5, 9)
    host = "https://your-application.com"

    @task
    def complete_journey(self):
        # Login
        self.client.post("/login", {"username": "user1", "password": "password1"}, name="Login")

        # Browse a few pages
        self.client.get("/dashboard", name="View Dashboard")
        self.client.get("/products", name="View Products")
        self.client.get("/products/123", name="View Specific Product")

        # Add to cart (example with POST data)
        self.client.post("/cart/add", json={"productId": 123, "quantity": 1}, name="Add to Cart")

        # Checkout
        self.client.post("/checkout", name="Checkout")

        # Logout
        self.client.get("/logout", name="Logout")
```

### 2. Using Request Parameters and Headers

```python
from locust import HttpUser, task, between

class APIUser(HttpUser):
    wait_time = between(1, 3)
    host = "https://api.your-service.com"

    @task
    def get_data_with_params(self):
        self.client.get("/data", params={"id": 123, "type": "report"}, name="Get Data with Params")

    @task
    def post_data_with_headers(self):
        headers = {"Authorization": "Bearer your_token_here", "Content-Type": "application/json"}
        payload = {"key": "value", "another_key": "another_value"}
        self.client.post("/resource", json=payload, headers=headers, name="Post Data with Headers")
```

### 3. Error Handling and Custom Metrics

```python
from locust import HttpUser, task, between, events

@events.request.add_listener
def my_request_handler(request_type, name, response_time, response_length, response,
                       context, exception, start_time, url, **kwargs):
    if exception:
        print(f"Request to {name} failed with exception {exception}")
    else:
        if response.status_code >= 400:
            print(f"Request to {name} returned status code {response.status_code}")

class ErrorHandlingUser(HttpUser):
    wait_time = between(1, 2)
    host = "http://localhost:8080"

    @task
    def sometimes_fails(self):
        # Simulate a request that might sometimes fail
        self.client.get("/random_status", name="Random Status Page")

    @task
    def check_specific_content(self):
        with self.client.get("/data_page", catch_response=True) as response:
            if "expected content" not in response.text:
                response.failure("Content validation failed")
            else:
                response.success()
```

This comprehensive update should significantly improve the `README.md` file.




