import streamlit as st
import os
import requests

#Override with an environment variable if the backend isn't on localhost.
BASE_URL = os.environ.get("SYNCPOINT_API_URL", "http://localhost:8080")

TIMEOUT_SECONDS = 5

def _session():
    if "api_session" not in st.session_state:
        st.session_state["api_session"] = requests.Session()
    return st.session_state["api_session"]

def reset_api_session():
    old = st.session_state.pop("api_session", None)
    if old is not None:
        old.close()

#Helpers

def _clean(value):
    if isinstance(value, str):
        value = value.strip()
        return value or None
    return value

def _error_message(status_code, data):
    if isinstance(data, dict):
        message = data.get("error")
        fields = data.get("fields")
        if message and isinstance(fields, dict) and fields:
            details = "; ".join(f"{name}: {problem}" for name, problem in fields.items())
            return f"{message} ({details})"

        if message:
            return message
    if status_code in (401, 403):
        return (
            f"Not authorized (backend returned {status_code})."
            "If you have just logged in, the backend login may not be creating a session yet."
        )
    return f"Request failed ({status_code})."

def _request(method, path, **kwargs):
    try:
        response = _session().request(
            method, f"{BASE_URL}{path}", timeout= TIMEOUT_SECONDS, **kwargs
        )
    except requests.exceptions.RequestException:
        return False, {"error": f"Could not reach the backend. Is it running on {BASE_URL}?"}
    if response.status_code == 204:
        return True, {}

    try:
        data = response.json()
    except ValueError:
        data = {}
    if  response.status_code in (200, 201):
        return True, data
    
    return False, {"error": _error_message(response.status_code, data)}

#calls the POST/api/auth/login/admin

#AUTH

def login_patient(username: str, password: str):
    #200 means {loginId, userId, patientId, displayName}
    #401 means wrong credentials, 403 registration pending/ rejected
    return _request("POST", "/api/auth/login/paatient", json= {"username": username, "password": password})

def login_staff(username: str, password: str):
    return _request("POST", "/api/auth/login/staff", json= {"username": username, "password": password})



def login_admin(username: str, password: str):
    try:
        response = requests.post(
            f"{BASE_URL}/api/auth/login/admin",
            json= {"username": username, "password": password},
            timeout= TIMEOUT_SECONDS,
        )
    except requests.exceptions.RequestException:
        return False, {"error": f"Could not reach the backend. Is it running on {BASE_URL}?"}

    try:
        data = response.json()
    except ValueError:
        data = {}

    if response.status_code == 200:
        return True, data

    return False, {"error": data.get("error", "Login Failed")}

#Calls POST/api/auth/logout, login endpoints never establish a real session either so this
#call will also 401 until that's fixed.

def logout(login_id):
    try:
        requests.post(
            f"{BASE_URL}/api/auth/logout",
            params={"loginId": login_id},
            timeout= TIMEOUT_SECONDS,
        )
    except requests.exceptions.RequestException:
        pass

#Calls GET/api/documents/staff-search. Used for Admin "Documents Reports" tab

def get_documents_for_staff(searching_user_id, search_term: str | None = None):
    params = {"searchingUserId": searching_user_id}
    if search_term:
        params["search"] = search_term
    return _request("GET", "/api/documents/staff-search", params= params)

#Used for the Staff overview/ Reports tabs

def get_staff_dashboard_stats():
    return _request("GET", "/api/dashboard/staff-stats")

#Used for staff "Manage Patient Documents" tab
def get_pending_documents_for_staff():
    return _request("GET", "/api/documents/pending-review")

#calls GET /api/patients/{patientId}/profile
def get_patient_profile(patient_id):
    return _request("GET", f"/api/patients/{patient_id}/profile")

#calls GET /api/documents/patient/{patientId}.....Used for the patient "My Documents"tab

def get_patient_documents(patient_id, search_term: str | None = None):
    params = {}
    if search_term:
        params["search"] = search_term

    return _request("GET", f"/api/documents/patient/{patient_id}", params= params)

#Used for patient
#Overview (pending- request check)

def get_document_requests_for_patient(patient_id):
    return _request("GET", f"/api/document-requests/patient/{patient_id}")

#"Approved/Rejected" Rejected True on success
def review_document(document_id, new_status: str, reviewed_by_user_id, rejection_reason: str | None = None):
    return _request(
        "PUT",
        f"/api/documents/{document_id}/review",
        json= {
            "reviewedByUserId": reviewed_by_user_id,
            "newStatus": new_status,
            "rejectionReason": rejection_reason,
        },
    )