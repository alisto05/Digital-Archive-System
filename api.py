import requests
import streamlit as st
from api_client import ApiClient, ApiError

def _Browser_ip():
    try:
        return st.context.ip_address
    except Exception:
        return None

#One ApiClient per browser session
def get_api() -> ApiClient:
    if "api" not in st.session_state:
        st.session_state["api"] = ApiClient(client_ip= _Browser_ip())
    return st.session_state["api"]

