import streamlit as st
from nav import nav_bar

nav_bar()
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")
st.write("")
st.divider()

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

if st.session_state.selected_role == "Patient":
    username = st.text_input("Enter Your Patient Username")
    password = st.text_input("Enter Your Password", type= "password")
    #DEMO
    if st.button("Login"):
        if username == "Alisto" and password == "password12":
            st.session_state.logged_in_user = username
            st.switch_page("pages/4_Dashboard.py")
        else:
            st.write("Login Failed")
elif st.session_state.selected_role == "Staff":
    staff_username = st.text_input("Enter Your Staff Username")
    staff_password = st.text_input("Enter Your Password", type= "password")
    #DEMO
    if st.button("Login"):
        if staff_username == "S-F-001" and staff_password == "@pass12":
            st.write("Login Successful")
        else:
            st.write("Login Failed")
elif st.session_state.selected_role == "Admin":
    admin_username = st.text_input("Enter Your Admin Username")
    admin_password = st.text_input("Enter Your Password", type= "password")
    #DEMO
    if st.button("Login"):
        if admin_username == "A-N-001" and admin_password == "admin12":
            st.write("Login Successful")
        else:
            st.write("Login Failed")