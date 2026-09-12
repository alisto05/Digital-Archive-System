import streamlit as st
from nav import nav_bar
from db import get_pending_patients, update_patient_status

nav_bar()

if st.session_state.get("logged_in_role") != "Staff":
    st.warning("Please log in as Staff to access this page.")
    if st.button("Go to Login"):
        st.switch_page("pages/1_Login.py")
    st.stop()

st.title("Pending Patient Registrations")

pending = get_pending_patients()
