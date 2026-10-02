import requests
import streamlit as st
from api_client import ApiClient, ApiError

def _browser_ip():
    try:
        return st.context.ip_address
    except Exception:
        return None

#One ApiClient per browser session
def get_api() -> ApiClient:
    if "api" not in st.session_state:
        st.session_state["api"] = ApiClient(client_ip= _browser_ip())
    return st.session_state["api"]

#Stops the page unless someone is logged in
def require_role(*roles) -> ApiClient:
    api = get_api()
    if api.user is None:
        st.warning("Please log in first.")
        st.stop()
    if roles and api.user["role"] not in roles:
        st.error("Your Account cannot open this page.")
        st.stop()
    return api

#Runs the API call and turns failure into on screen messages using the backend's own error text
def call(fn, *args, stop = True, **kwargs):
    api = get_api()
    try:
        return fn(*args, **kwargs)
    except ApiError as e:
        if e.status == 401 and api.user is not None:
            api.reset()
            st.warning("Your session expired. Please log in again.")
        else:
            st.error(e.message)
    except requests.RequestException:
        st.error("Cannot reach the backend. Is it still running?")
    if stop:
        st.stop()
    return None