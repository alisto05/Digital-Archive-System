import os
import requests

#Override with an environment variable if the backend isn't on localhost.
BASE_URL = os.environ.get("SYNCPOINT_API_URL", "http://localhost:8080")

TIMEOUT_SECONDS = 5

