import streamlit as st
from nav import nav_bar
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

nav_bar()

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

if st.session_state.selected_role == "Patient":
    tab1, tab2, tab3, tab4 = st.tabs(["Overview", "My profile", "My Documents", "Recent Activity"])


    with tab1:
        st.header(f"Welcome back, {st.session_state.logged_in_user}")
        st.warning("You have 1 pending document request. See 'My Documents' tab.")
        
    with tab2:
        st.caption("**Title**")
        st.write("Mr")
        st.caption("**First Name**")
        st.write("Alizwa")
        st.caption("**Middle Name**")
        st.write("")
        st.caption("**Date of Birth**")
        st.write("**YYYY-MM-DD**")
        st.caption("**ID Number**")
        st.write("")
        st.caption("**Address**")
        st.write("")
        st.caption("**City**")
        st.write("")
        st.caption("**Province**")
        st.write("")
        st.caption("**Phone Number**")
        st.write("")
        st.caption("**Email**")
        st.write("")
        st.button("REQUEST CHANGE")

    with tab3:
        st.subheader("Requested Documents")
        documents = [
            {"Document Needed": "Proof of Residence", "Requested by": "Staff", "Status": "Pending"},
        ]
        st.dataframe(documents)
        search_term = st.text_input("Search your documents")

        st.subheader("Upload a document")
        upoloaded_file = st.file_uploader("Upload here", type= ["pdf"])

    with tab4:
        st.subheader("Recent Activity")
        activity = [
            {"Action": "Lab Report viewed", "By": "Dr. Smith", "Date": "2026-08-03"},
            {"Action": "ID Document uploaded", "By": "You", "Date": "2026-08-02"},
        ]
        st.dataframe(activity)

st.page_link("home.py", label= "Back to Home")

