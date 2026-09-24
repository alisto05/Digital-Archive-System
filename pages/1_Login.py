import streamlit as st
from nav import nav_bar
from db import get_patient_login_data, get_staff_login_data, verify_password

nav_bar()

if "logged_in_role" not in st.session_state:
    st.session_state.logged_in_role = None

#This send the Patient or the Staff directly to their dashboard,
#if they logged in successful
if st.session_state.logged_in_role:
    st.switch_page("pages/4_Dashboard.py")

#This tracks which or who is logging in(patient or Staff) 
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")
st.write("")
st.divider()

#This separates the Patient and Staff using a border
col1, col2 = st.columns(2, border= True)
with col1:
    st.image("assets/patient-removebg.png", width= 50, use_container_width= True)
    if st.button("Patient Login"):
        st.session_state.selected_role = "Patient"
    st.caption("Registered patient Login Here")

with col2:
    st.image("assets/staff.png", width= 50, use_container_width= True)
    if st.button("Staff Login"):
        st.session_state.selected_role = "Staff"
    st.caption("Staff Login Here")

#If the Pateient was clicked, this how it would look like
if st.session_state.selected_role == "Patient":
    username = st.text_input("Enter Your Patient Username")
    password = st.text_input("Enter Your Password", type= "password")
    
    #DEMO
    if st.button("Login"):
        record = get_patient_login_data(username)

        if record is None:
            st.error("Login Failed")
        elif not verify_password(password, record["password_hash"]):
            st.error("Login Failed")
        elif record["status"] == "PENDING":
            st.warning("Your registration is still awaiting Staff Approval.")
        elif record["status"] == "REJECTED":
            st.error("Your Registration was not approved. Contact SyncPoint support.")

        else:
            st.session_state.logged_in_user = record["preferred_name"] or username
            st.session_state.logged_in_role = "Patient"
            st.session_state.patient_id = record["patient_id"]
            st.switch_page("pages/4_Dashboard.py")
elif st.session_state.selected_role == "Staff":
    staff_username = st.text_input("Enter Your Staff Username")
    staff_password = st.text_input("Enter Your Password", type= "password")

    if st.button("Login"):
        record = get_staff_login_data(staff_username)

        if record is None:
            st.error("Login Failed")
        elif not verify_password(staff_password, record["password_hash"]):
            st.error("Login Failed")
        else:
            st.session_state.logged_in_user = f"{record['first_name']} {record['last_name']}"
            st.session_state.logged_in_role = "Staff"
            st.session_state.staff_id = record["staff_id"]
            st.switch_page("pages/4_Dashboard.py") 
          
            