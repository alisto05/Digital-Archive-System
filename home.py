import streamlit as st
from nav import nav_bar, footer

nav_bar()

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")
title = st.title("Welcome to SyncPoint")

left, middle, right = st.columns([1, 2, 1])
with middle:
    st.image("assets/logo-removebg.png", use_container_width= True)
    subheader = st.subheader("Hospital Digital Archive System")
    st.write("Hospital operations by improving data accessibility, reducing risks of lost or misplaced records, and enabling efficient reporting for hospital administration")

st.write("")
st.divider()

st.header("Manage your health record in 3 simple steps.")
st.write("Upload, search and access yor documents")

col1, col2, col3 = st.columns(3)
with col1:
    st.caption("STEP 01")
    st.image("assets/Upload-logo.png", use_container_width= True)
    st.subheader("Upload")
    st.write("Upload your document securely")

with col2:
    st.caption("STEP 02")
    st.image("assets/search.png", use_container_width= True)
    st.subheader("Search")
    st.write("Search your documents that you uploaded")

with col3:
    st.caption("STEP 03")
    st.image("assets/download.png", use_container_width= True)
    st.subheader("View/ Download")
    st.write("Access your documents")

st.write("")
st.write("")
st.divider()
footer()