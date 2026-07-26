import streamlit as st
from nav import nav_bar

nav_bar()

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")
st.image("assets/logo-removebg.png", use_container_width= True)

left, middle, right = st.columns([1, 2, 1])
with middle:
    title = st.title("Welcome to SyncPoint")
    subheader = st.subheader("Hospital Digital Archive System")
    st.write("Hospital operations by improving data accessibility, reducing risks of lost or misplaced records, and enabling efficient reporting for hospital administration")