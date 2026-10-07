import streamlit as st
from backend_api import logout
from nav import footer

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

st.warning("Are you sure you want to sign out?")
col1, col2 = st.columns(2)
with col1:
    if st.button("Yes"):
        logout()

        st.session_state.selected_role = None
        st.session_state.logged_in_user = None
        st.session_state.logged_in_role = None

        for key in ("login_id", "user_id", "patient_id", "staff_id", "admin_id"):
            st.session_state.pop(key, None)

        st.switch_page("home.py")
with col2:
    if st.button("Cancel"):
        st.switch_page("pages/4_Dashboard.py")
        
st.stop()

footer()