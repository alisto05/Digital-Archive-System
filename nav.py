import base64
from pathlib import Path

import streamlit as st


def load_css(path="assets/home.css"):
    """Read a CSS file and inject it into the page."""
    css = Path(path).read_text(encoding="utf-8")
    st.markdown(f"<style>{css}</style>", unsafe_allow_html=True)


def img_src(path):
    """Turn an image file into text that an HTML <img src="..."> can use."""
    data = base64.b64encode(Path(path).read_bytes()).decode()
    return f"data:image/png;base64,{data}"


def nav_bar():
    st.markdown(
        "<style>[data-testid='stSidebarNav'] {display: none;}</style>",
        unsafe_allow_html=True
    )

    with st.sidebar:
        st.page_link("home.py", label="Home")
        st.page_link("pages/1_Login.py", label="Login")
        st.page_link("pages/2_Register.py", label="Register")
        st.page_link("pages/3_Help.py", label="Help")
        
    col1, col2, col3, col4 = st.columns(4)
    with col1:
        st.page_link("home.py", label= "Home")
    with col2:
        st.page_link("pages/1_Login.py", label= "Login")
    with col3:
        st.page_link("pages/2_Register.py", label= "Register")
    with col4:
        st.page_link("pages/3_Help.py", label= "Help")


def footer():
    col1, col2, col3, col4 = st.columns(4)
    with col1:
        st.image("assets/logo-removebg.png", use_container_width= True)
    with col2:
        st.write("**About SyncPoint**")
        st.page_link("pages/3_Help.py", label= "Contact Us", use_container_width= True)
        st.page_link("pages/3_Help.py", label= "About Us", use_container_width= True)
    with col3:
        st.write("**For Patients**")
        st.page_link("pages/2_Register.py", label= "Register", use_container_width= True)
        st.page_link("pages/1_Login.py", label= "Login", use_container_width= True)
    with col4:
        st.write("**For Staff**")
        st.page_link("pages/1_Login.py", label= "Staff Login", use_container_width= True)
