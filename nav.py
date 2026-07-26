import streamlit as st

def nav_bar():
    col1, col2, col3, col4, col5, col6, col7 = st.columns(7)
    with col1:
        st.page_link("home.py", label= "Home")
    with col2:
        st.page_link("pages/1_Login.py", label= "Login")
    with col3:
        st.page_link("pages/2_Register.py", label= "Register")
    with col4:
        st.page_link("pages/3_Help.py", label= "Help")
    with col5:
        st.page_link("pages/4_Dashboard.py", label= "Dashboard")
    with col6:
        st.page_link("pages/5_Search.py", label= "Search")
    with col7:
        st.page_link("pages/6_Upload.py", label= "Upload")