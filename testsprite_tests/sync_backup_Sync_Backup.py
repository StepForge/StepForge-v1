import requests
import json
import datetime

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

# Replace with a valid token for authentication
VALID_AUTH_TOKEN = "Bearer VALID_AUTH_TOKEN_EXAMPLE"
INVALID_AUTH_TOKEN = "Bearer INVALID_AUTH_TOKEN_EXAMPLE"

def sync_backup():
    headers_auth = {
        "Authorization": VALID_AUTH_TOKEN,
        "Content-Type": "application/json"
    }
    headers_no_auth = {
        "Content-Type": "application/json"
    }
    headers_invalid_auth = {
        "Authorization": INVALID_AUTH_TOKEN,
        "Content-Type": "application/json"
    }

    test_data = {
        "data": {
            "backup_timestamp": datetime.datetime.utcnow().isoformat() + "Z",
            "steps": 12345,
            "user_settings": {
                "units": "metric",
                "daily_step_goal": 10000,
                "sync_preferences": {
                    "auto_sync": True,
                    "sync_interval": 60
                }
            }
        }
    }

    # 1) Test successful backup with valid auth
    try:
        resp = requests.post(f"{BASE_URL}/api/backup", headers=headers_auth, json=test_data, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly: {e}"
    assert resp.status_code == 200, f"Expected 200 OK, got {resp.status_code}"

    # 2) Test backup with server-side failure simulation (simulate by sending a special flag)
    failure_data = {
        "data": {
            "simulate_failure": True
        }
    }
    try:
        resp_fail = requests.post(f"{BASE_URL}/api/backup", headers=headers_auth, json=failure_data, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly during failure simulation: {e}"
    assert resp_fail.status_code == 500, f"Expected 500 Internal Server Error, got {resp_fail.status_code}"
    assert resp_fail.text.strip() == "Backup failed", "Response body mismatch for failure message"

    # 3) Test backup with missing Authorization header
    try:
        resp_no_auth = requests.post(f"{BASE_URL}/api/backup", headers=headers_no_auth, json=test_data, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly without auth: {e}"
    assert resp_no_auth.status_code == 401, f"Expected 401 Unauthorized, got {resp_no_auth.status_code}"

    # 4) Test backup with invalid Authorization token
    try:
        resp_invalid_auth = requests.post(f"{BASE_URL}/api/backup", headers=headers_invalid_auth, json=test_data, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly with invalid auth: {e}"
    assert resp_invalid_auth.status_code == 401, f"Expected 401 Unauthorized on invalid token, got {resp_invalid_auth.status_code}"

    # 5) Additional: test that data is correctly backed up by verifying GET steps and settings endpoints (sync backup dependency)
    # This part assumes related endpoints require same auth and return expected data after backup.
    # GET /api/steps?date=<today's date>
    today_str = datetime.datetime.utcnow().date().isoformat()
    try:
        resp_steps = requests.get(f"{BASE_URL}/api/steps?date={today_str}", headers=headers_auth, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly on GET /api/steps: {e}"
    assert resp_steps.status_code == 200, f"Expected 200 OK on GET /api/steps, got {resp_steps.status_code}"
    steps_data = resp_steps.json()
    assert isinstance(steps_data, dict), "Steps response is not a JSON object"
    assert "steps" in steps_data and isinstance(steps_data["steps"], int), "Steps count missing or invalid"
    assert "date" in steps_data and steps_data["date"] == today_str, "Date field missing or mismatch"

    # GET /api/settings
    try:
        resp_settings = requests.get(f"{BASE_URL}/api/settings", headers=headers_auth, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly on GET /api/settings: {e}"
    assert resp_settings.status_code == 200, f"Expected 200 OK on GET /api/settings, got {resp_settings.status_code}"
    settings = resp_settings.json()
    assert isinstance(settings, dict), "Settings response is not a JSON object"
    assert "units" in settings and settings["units"] in ["metric", "imperial"], "Units field invalid or missing"
    assert "daily_step_goal" in settings and isinstance(settings["daily_step_goal"], int), "Daily step goal invalid or missing"

    # PUT /api/settings to update settings
    new_settings = settings.copy()
    new_settings["daily_step_goal"] = new_settings.get("daily_step_goal", 10000) + 1000
    try:
        resp_put = requests.put(f"{BASE_URL}/api/settings", headers=headers_auth, json=new_settings, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly on PUT /api/settings: {e}"
    assert resp_put.status_code == 200, f"Expected 200 OK on PUT /api/settings, got {resp_put.status_code}"
    put_response_text = resp_put.text.strip()
    assert put_response_text == "Updated", "PUT /api/settings response mismatch"

    # GET with invalid token on /api/settings to check 401
    try:
        resp_settings_unauth = requests.get(f"{BASE_URL}/api/settings", headers=headers_invalid_auth, timeout=TIMEOUT)
    except requests.RequestException as e:
        assert False, f"Request failed unexpectedly on GET /api/settings with invalid auth: {e}"
    assert resp_settings_unauth.status_code == 401, f"Expected 401 Unauthorized on GET /api/settings with invalid token, got {resp_settings_unauth.status_code}"


sync_backup()
