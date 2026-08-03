import streamlit as st
from nav import nav_bar

nav_bar()
st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")