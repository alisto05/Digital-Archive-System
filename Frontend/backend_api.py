import streamlit as st
import os
import requests
from typing import Any

#Override with an environment variable if the backend isn't on localhost.
BASE_URL = os.environ.get("SYNCPOINT_API_URL", "http://localhost:8080")

TIMEOUT_SECONDS = 5

def _session():
    if "api_session" not in st.session_state:
        st.session_state["api_session"] = requests.Session()
    return st.session_state["api_session"]

def reset_api_session():
    old = st.session_state.pop("api_session", None)
    st.session_state.pop("csrf", None)
    if old is not None:
        old.close()

#The backend requires a CSRF token on every POST/ PUT/ DELETE
def _csrf_headers(refresh= False):
    if refresh:
        st.session_state.pop("csrf", None)
    if "csrf" not in st.session_state:
        response = _session().get(f"{BASE_URL}/api/auth/csrf", timeout= TIMEOUT_SECONDS)
        response.raise_for_status()
        data = response.json()
        st.session_state["csrf"] = (data["headerName"], data["token"])
    header_name, token = st.session_state["csrf"]
    return {header_name: token}

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
            " If you have just logged in, the backend login may not be creating a session yet."
        )
    return f"Request failed ({status_code})."

def _request(method, path, **kwargs) -> tuple[bool, Any]:
    changes_data = method.upper() in ("POST", "PUT", "PATCH", "DELETE")
    base_headers = dict(kwargs.pop("headers", None) or {})
    try:
        for attempt in (1, 2):
            headers = dict(base_headers)
            if changes_data:
                headers.update(_csrf_headers(refresh = attempt == 2))
            response = _session().request(
                method, f"{BASE_URL}{path}", timeout= TIMEOUT_SECONDS, headers= headers, **kwargs
            )
            #A rejected token is fetched again once
            if not (changes_data and response.status_code == 403 and "CSRF" in response.text):
                break
        
    except (requests.exceptions.RequestException, KeyError, ValueError):
        return False, {"error": f"Could not reach the backend. Is it running on {BASE_URL}?"}
    
    #The backend rotates the token on login so that it is fetched again next time
    if path.startswith("/api/auth/login"):
        st.session_state.pop("csrf", None)
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
    return _request("POST", "/api/auth/login/patient", json= {"username": username, "password": password})

def login_staff(username: str, password: str):
    return _request("POST", "/api/auth/login/staff", json= {"username": username, "password": password})



def login_admin(username: str, password: str):
    return _request("POST", "/api/auth/login/admin", json= {"username": username, "password": password})

def logout():
    try:
        _session().post(
            f"{BASE_URL}/api/auth/logout",
            headers= _csrf_headers(),
            timeout= TIMEOUT_SECONDS,
        )
    except requests.exceptions.RequestException:
        pass
    reset_api_session()

#PAITENT REGI

def register_patient(
        username, password, title, first_name, middle_name, last_name, preferred_name, id_number,
        date_of_birth, country, address_line, city, postal_code, province, home_phone, work_phone,
        mobile_phone, secondary_phone, email, secondary_email, has_medical_aid, provider, membership_number,
):
    payload = {
        "username": _clean(username),
        "password": password,
        "title": _clean(title),
        "firstName": _clean(first_name),
        "middleName": _clean(middle_name),
        "lastName": _clean(last_name),
        "preferredName": _clean(preferred_name),
        "idNumber": _clean(id_number),
        "dateOfBirth": date_of_birth,
        "country": _clean(country),
        "addressLine": _clean(address_line),
        "city": _clean(city),
        "postalCode": _clean(postal_code),
        "province": _clean(province),
        "homePhone": _clean(home_phone),
        "workPhone": _clean(work_phone),
        "mobilePhone": _clean(mobile_phone),
        "secondaryPhone": _clean(secondary_phone),
        "email": _clean(email),
        "secondaryEmail": _clean(secondary_email),
        "hasMedicalAid": bool(has_medical_aid),
        "provider": _clean(provider),
        "membershipNumber": _clean(membership_number),
    }
    return _request("POST", "/api/patients/register", json= payload)

#STAFF: patient approvals

def get_pending_patients():
    return _request("GET", "/api/approvals/pending-patients")

def update_patient_status(patient_id, new_status: str):
    return _request(
        "PUT",
        f"/api/approvals/patients/{patient_id}/status",
        json= {"newStatus": new_status},
    )

def get_recent_activity_for_patient(patient_id):
    return _request("GET", f"/api/approvals/patients/{patient_id}/recent-activity")


#Calls GET/api/documents/staff-search. Used for Admin "Documents Reports" tab

def get_documents_for_staff(search_term: str | None = None):
    params = {}
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
def review_document(document_id, new_status: str, rejection_reason: str | None = None):
    return _request(
        "PUT",
        f"/api/documents/{document_id}/review",
        json= {
            "newStatus": new_status,
            "rejectionReason": rejection_reason,
        },
    )

def get_admin_overview():
    return _request("GET", "/api/admin/overview")

def get_all_staff():
    return _request("GET", "/api/admin/staff")

def get_all_patients():
    return _request("GET", "/api/admin/patients")