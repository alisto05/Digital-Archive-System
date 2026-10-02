import streamlit as st
from nav import nav_bar
from backend_api import login_patient, login_staff, login_admin

nav_bar()

if "logged_in_role" not in st.session_state:
    st.session_state.logged_in_role = None

#This send the Patient or the Staff directly to their dashboard, z
#if they logged in successful
if st.session_state.logged_in_role:
    st.switch_page("pages/4_Dashboard.py")

#This tracks which or who is logging in(patient or Staff) 
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

message = st.session_state.pop("message", None)
if message:
    st.success(message)

st.write("")
st.divider()

#This separates the Patient and Staff using a border
col1, col2, col3 = st.columns(3, border= True)
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

with col3:
    st.image("assets/admin-removebg.png", width= 50, use_container_width= True)
    if st.button("Admin Login"):
        st.session_state.selected_role = "Admin"
    st.caption("Admin Login Here")

def finish_login(role, data):
    #Keeps the same session for every role so other page can rely on them
    st.session_state.logged_in_user = data["displayName"]
    st.session_state.logged_in_role = role
    st.session_state.user_id = data.get["loginId"]
    st.session_state.login_id = data.get("loginId")
    st.switch_page("pages/4_Dashboard.py")

#If the Pateient was clicked, this how it would look like
if st.session_state.selected_role == "Patient":
    username = st.text_input("Enter Your Patient Username")
    password = st.text_input("Enter Your Password", type= "password")
    
    
    if st.button("Login", key= "patient_login_button"):
        success, data = login_patient(username, password)

        if not success:
            st.error(data.get("error", "Login Failed"))
        else:
            st.session_state.patient_id = data["patientId"]
            finish_login("Patient", data)
            
elif st.session_state.selected_role == "Staff":
    staff_username = st.text_input("Enter Your Staff Username")
    staff_password = st.text_input("Enter Your Password", type= "password", key= "staff_password")

    if st.button("Login", key= "staff_login_button"):
        success, data = login_staff(staff_username, staff_password)
        if not success:
            st.error(data.get("error", "Login Failed"))
        else:
            st.session_state.staff_id = data["staffId"]
            finish_login("Staff", data)

elif st.session_state.selected_role == "Admin":
    admin_username = st.text_input("Enter Your Admin Username")
    admin_password = st.text_input("Enter Your Password", type= "password", key= "admin_password")

    #This one goes through thee backend
    if st.button("Login", key= "admin_login_button"):
        success, data = login_admin(admin_username, admin_password)

        if not success:
            st.error(data.get("error", "Login Failed"))
        else:
            st.session_state.admin_id = data["adminId"]
            finish_login("Admin", data)