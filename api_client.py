#Rules the backend enforces, handled here so pages don't have to care:
#every POST/ PUT needs the CSRF token in the header the backend names (GET /api/auth/csrf)

import os
import requests

#for any http code/ status >= 400

class ApiError(Exception):
    def __init__(self, status, message):
        super().__init__(message)
        self.status = status
        self.message = message

class ApiClient:
    def __init__(self, base_url = None, client_ip = None, timeout = 20):
        self.base_url = (base_url or os.environ.get("API_BASE_URL", "http://localhost:8080")).rstrip("/")
        self.client_ip = client_ip
        self.timeout = timeout
        self.http = requests.Session()
        self._csrf = None
        self.user = None

    #AUTH
    #role is one of either patient, staff, admin

    def login(self, role, username, password):
        self.user = self.post(f"/api/auth/login/{role}", json = {"username": username, "password": password}).json()
        self._csrf = None
        return self.user

    def logout(self):
        try:
            self.post("/api/auth/logout")
        finally:
            self.reset()

    def reset(self):
        self.http.cookies.clear()
        self._csrf = None
        self.user = None

# =========== verbs ======================
    def get(self, path, **kw):
        return self.request("GET", path, **kw)

    def post(self, path, **kw):
        return self.request("POST", path, **kw)

    def put(self, path, **kw):
        return self.request("PUT", path, **kw)

#Sends the request for file uploads
    def request(self, method, path, *, _retry = True, **kw):
        method = method.upper()
        changes_data = method not in ("GET", "HEAD", "OPTIONS")
        headers = dict(kw.pop("headers", {}) or {})
        if self.client_ip:
            headers["X-Forwarded-For"] = self.client_ip
        if changes_data:
            if self._csrf is None:
                self._fetch_csrf()
            headers[self._csrf[0]] = self._csrf[1]
            
        resp = self.http.request(method, self.base_url + path, headers = headers, timeout = self.timeout, **kw)

    #When the token went on stale like session expired and recreated it fetches a new one and retry once

        if changes_data and _retry and resp.status_code == 403 and "CSRF" in resp.text:
            self._csrf = None
            return self.request(method, path, _retry = False, headers = headers, **kw)
        if resp.status_code >= 400:
            try:
                message = resp.json().get("error") or resp.reason
            except ValueError:
                message = resp.reason or "Request failed."
            raise ApiError(resp.status_code, message)
        return resp

    def _fetch_csrf(self):
        resp = self.http.get(f"{self.base_url}/api/auth/csrf", timeout = self.timeout)
        if resp.status_code >= 400:
            raise ApiError(resp.status_code, "Could not get a CSRF token from the backend.")
        data = resp.json()
        self._csrf = (data["headerName"], data["token"])