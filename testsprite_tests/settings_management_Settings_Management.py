import requests
import datetime
import json

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

# Replace with a valid Authorization token for positive tests
VALID_TOKEN = "Bearer VALID_AUTH_TOKEN"
# Intentionally invalid token for negative tests
INVALID_TOKEN = "Bearer INVALID_AUTH_TOKEN"

def test_settings_management():
    headers_valid = {
        "Authorization": VALID_TOKEN,
        "Content-Type": "application/json"
    }
    headers_invalid = {
        "Authorization": INVALID_TOKEN,
        "Content-Type": "application/json"
    }

    # 1) GET /api/settings with valid Authorization bearer token -> Receive 200 Settings object
    try:
        r = requests.get(f"{BASE_URL}/api/settings", headers=headers_valid, timeout=TIMEOUT)
        assert r.status_code == 200, f"Expected 200 OK, got {r.status_code}"
        settings = r.json()
        # Basic validation of settings object keys - expect dictionary
        assert isinstance(settings, dict), "Settings response should be a JSON object"
    except Exception as e:
        raise AssertionError(f"GET /api/settings with valid token failed: {e}")

    # 2) PUT /api/settings with body <Settings object> and valid Authorization bearer token -> Receive 200 Updated
    # Modify settings slightly or send valid settings update
    try:
        # Prepare update payload - use existing settings but modify one field if exists
        updated_settings = settings.copy()
        # Example fields to update to valid values for testing; fallback defaults
        if "units" in updated_settings:
            updated_settings["units"] = "metric" if updated_settings["units"] != "metric" else "imperial"
        else:
            updated_settings["units"] = "metric"
        if "dailyStepGoal" in updated_settings:
            updated_settings["dailyStepGoal"] = max(1000, updated_settings.get("dailyStepGoal",1000))
        else:
            updated_settings["dailyStepGoal"] = 10000
        if "syncPreferences" in updated_settings:
            updated_settings["syncPreferences"] = {"wifiOnly": True}
        else:
            updated_settings["syncPreferences"] = {"wifiOnly": True}

        r = requests.put(
            f"{BASE_URL}/api/settings",
            headers=headers_valid,
            data=json.dumps(updated_settings),
            timeout=TIMEOUT
        )
        assert r.status_code == 200, f"Expected 200 OK for valid update, got {r.status_code}"
    except Exception as e:
        raise AssertionError(f"PUT /api/settings with valid body failed: {e}")

    # 3) PUT /api/settings with malformed body (invalid fields/types) and valid Authorization bearer token -> Receive 400 Bad Request
    try:
        malformed_body = {
            "units": 123,                 # invalid type, expecting string
            "dailyStepGoal": "ten thousand",  # invalid type, expecting number
            "extraInvalidField": ["bad", "data"]  # invalid unexpected field
        }
        r = requests.put(
            f"{BASE_URL}/api/settings",
            headers=headers_valid,
            data=json.dumps(malformed_body),
            timeout=TIMEOUT
        )
        assert r.status_code == 400, f"Expected 400 Bad Request for malformed input, got {r.status_code}"
    except Exception as e:
        # The server might reject malformed JSON or schema, treat any exceptions as fail
        raise AssertionError(f"PUT /api/settings with malformed body failed: {e}")

    # 4) GET /api/settings with expired/invalid Authorization bearer token -> Receive 401 Unauthorized
    try:
        r = requests.get(f"{BASE_URL}/api/settings", headers=headers_invalid, timeout=TIMEOUT)
        assert r.status_code == 401, f"Expected 401 Unauthorized for invalid token, got {r.status_code}"
    except Exception as e:
        raise AssertionError(f"GET /api/settings with invalid token failed: {e}")


test_settings_management()