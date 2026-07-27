import streamlit as st
from nav import nav_bar

nav_bar()
st.write("")
st.divider()

col1, col2, col3 = st.columns(3)
with col1:
    st.image("assets/patient-logo.png", width= 50, use_container_width= True)
    st.write("Patient Login")
    st.caption("Registered patient Login Here")
with col2:
    st.image("assets/staff.png", width= 50, use_container_width= True)
    st.write("Staff Login")
    st.caption("Staff Login Here")
with col3:
    st.image("assets/admin.jpg", width= 50, use_container_width= True)
    st.write("Admin Login")
    st.caption("Admin Login Here")