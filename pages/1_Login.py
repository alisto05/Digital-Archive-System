import streamlit as st
from nav import nav_bar

nav_bar()
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None
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
    st.text_input("Enter Your Patient Username")
    st.text_input("Enter Your Password", type= "password")
elif st.session_state.selected_role == "Staff":
    st.text_input("Enter Your Staff Username")
    st.text_input("Enter Your Password", type= "password")
elif st.session_state.selected_role == "Admin":
    st.text_input("Enter Your Admin Username")
    st.text_input("Enter Your Password", type= "password")