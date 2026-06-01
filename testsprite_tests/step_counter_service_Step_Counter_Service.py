import requests
import datetime
import uuid

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

# Assuming we have some valid and invalid tokens for testing
VALID_TOKEN = "Bearer VALID_AUTH_TOKEN"
INVALID_TOKEN = "Bearer INVALID_AUTH_TOKEN"
MISSING_TOKEN = None


def test_step_counter_service():
    headers_auth = {"Authorization": VALID_TOKEN, "Content-Type": "application/json"}
    headers_no_auth = {"Content-Type": "application/json"}
    headers_invalid_auth = {"Authorization": INVALID_TOKEN, "Content-Type": "application/json"}

    # Helper function to post step sync
    def post_steps_sync(steps, ts, headers):
        url = f"{BASE_URL}/api/steps/sync"
        payload = {"steps": steps, "timestamp": ts}
        try:
            resp = requests.post(url, json=payload, headers=headers, timeout=TIMEOUT)
            return resp
        except requests.RequestException as e:
            raise RuntimeError(f"Request to {url} failed: {e}")

    # Helper function to get daily steps
    def get_daily_steps(date_str, headers):
        url = f"{BASE_URL}/api/steps"
        params = {"date": date_str}
        try:
            resp = requests.get(url, headers=headers, params=params, timeout=TIMEOUT)
            return resp
        except requests.RequestException as e:
            raise RuntimeError(f"Request to {url} failed: {e}")

    # Current ISO timestamp str
    timestamp_now = datetime.datetime.utcnow().isoformat() + "Z"
    date_today = datetime.datetime.utcnow().strftime("%Y-%m-%d")

    # 1) POST /api/steps/sync with valid auth and normal steps > expect 200 Success
    resp = post_steps_sync(1234, timestamp_now, headers_auth)
    assert resp.status_code == 200, f"Expected 200 OK, got {resp.status_code}"

    # 2) GET /api/steps with valid auth for today > expect 200 and correct schema with steps and date
    resp = get_daily_steps(date_today, headers_auth)
    assert resp.status_code == 200, f"Expected 200 OK, got {resp.status_code}"
    json_data = resp.json()
    assert "steps" in json_data and isinstance(json_data["steps"], (int, float)), "Missing or invalid 'steps'"
    assert "date" in json_data and isinstance(json_data["date"], str), "Missing or invalid 'date'"
    assert json_data["date"] == date_today, "Returned date does not match query date"

    # 3) POST /api/steps/sync with steps=0 edge case > expect 200 Success
    timestamp_zero = (datetime.datetime.utcnow() - datetime.timedelta(minutes=5)).isoformat() + "Z"
    resp = post_steps_sync(0, timestamp_zero, headers_auth)
    assert resp.status_code == 200, f"Zero steps sync expected 200 OK, got {resp.status_code}"

    # 4) POST /api/steps/sync with very large steps > expect 200 Success (verify no error)
    large_steps = 10_000_000
    timestamp_large = (datetime.datetime.utcnow() - datetime.timedelta(minutes=10)).isoformat() + "Z"
    resp = post_steps_sync(large_steps, timestamp_large, headers_auth)
    assert resp.status_code == 200, f"Large steps sync expected 200 OK, got {resp.status_code}"

    # 5) POST /api/steps/sync with missing Authorization > expect 401 Unauthorized
    resp = post_steps_sync(1000, timestamp_now, headers_no_auth)
    assert resp.status_code == 401, f"Missing auth expected 401 Unauthorized, got {resp.status_code}"

    # 6) POST /api/steps/sync with invalid Authorization > expect 401 Unauthorized
    resp = post_steps_sync(1000, timestamp_now, headers_invalid_auth)
    assert resp.status_code == 401, f"Invalid auth expected 401 Unauthorized, got {resp.status_code}"

    # 7) GET /api/steps with missing Authorization > expect 401 Unauthorized
    resp = get_daily_steps(date_today, headers_no_auth)
    assert resp.status_code == 401, f"Missing auth GET expected 401 Unauthorized, got {resp.status_code}"

    # 8) GET /api/steps with invalid Authorization > expect 401 Unauthorized
    resp = get_daily_steps(date_today, headers_invalid_auth)
    assert resp.status_code == 401, f"Invalid auth GET expected 401 Unauthorized, got {resp.status_code}"

    # Additional Error Handling: malformed body test - send missing fields in POST /steps/sync
    url_sync = f"{BASE_URL}/api/steps/sync"
    malformed_payloads = [
        {},  # Empty body
        {"steps": "not_a_number", "timestamp": timestamp_now},
        {"steps": 100, "timestamp": 1234567890},  # timestamp not string
        {"steps": -5, "timestamp": timestamp_now},  # negative steps - depends on implementation, may accept or error
    ]
    for payload in malformed_payloads:
        try:
            resp = requests.post(url_sync, json=payload, headers=headers_auth, timeout=TIMEOUT)
            # Accepting 400 Bad Request for malformed, else if 200 or other proceed to check
            if resp.status_code == 400 or resp.status_code == 422:
                continue
            # If 200, no error but log
            assert resp.status_code in (200, 400, 422), f"Unexpected status {resp.status_code} for malformed payload {payload}"
        except requests.RequestException as e:
            raise RuntimeError(f"Malformed payload request failed: {e}")

    # Additional Data Synchronization Test
    # Sync multiple step batches for today and verify GET aggregates or shows recent
    steps_batches = [100, 200, 300]
    timestamps_batches = [
        (datetime.datetime.utcnow() - datetime.timedelta(minutes=20)).isoformat() + "Z",
        (datetime.datetime.utcnow() - datetime.timedelta(minutes=15)).isoformat() + "Z",
        (datetime.datetime.utcnow() - datetime.timedelta(minutes=10)).isoformat() + "Z",
    ]

    for s, ts in zip(steps_batches, timestamps_batches):
        resp = post_steps_sync(s, ts, headers_auth)
        assert resp.status_code == 200, f"Batch step sync expected 200 OK, got {resp.status_code}"

    resp = get_daily_steps(date_today, headers_auth)
    assert resp.status_code == 200, "Expected 200 OK after multiple batch syncs"
    data = resp.json()
    assert isinstance(data.get("steps"), (int, float)), "Aggregated steps missing or not a number"
    # The steps should be >= sum of batches (assuming backend accumulates)
    assert data["steps"] >= sum(steps_batches), "Steps aggregation seems inconsistent"

    print("All Step Counter Service tests passed.")


test_step_counter_service()