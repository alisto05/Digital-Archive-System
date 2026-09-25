import os
import requests

#Override with an environment variable if the backend isn't on localhost.
BASE_URL = os.environ.get("SYNCPOINT_API_URL", "http://localhost:8080")

TIMEOUT_SECONDS = 5

def _no_session_error(status_code):
    return (
        f"Not authorized (backend returned {status_code}). The login endpoint "
        "doesn't set up a real Spring Security session yet, so role-gated "
        "endpoints reject every request - flag this to the backend team."
    )

