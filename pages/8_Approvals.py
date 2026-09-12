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

if not pending:
    st.info("No pending registrations right now.")
else:
    for patient in pending:
        with st.container(border= True):
            st.write(f"**{patient['first_name']} {patient['last_name']}**")
            st.caption(f"ID Number: {patient['id_number']} | Registered: {patient['created_at']}")

            col1, col2 = st.columns(2)
            with col1:
                if st.button("Approve", key= f"approve_{patient['patient_id']}"):
                    update_patient_status(patient["patient_id"], "APPROVED")
                    st.rerun()

            with col2:
                if st.button("Reject", key= f"reject_{patient['patient_id']}"):
                    update_patient_status(patient["patient_id", "REJECTED"])
                    st.rerun()