import streamlit as st
from nav import nav_bar


if "all_documents" not in st.session_state:
    st.session_state.all_documents = [
            {"Patient": "Alisto", "Document Type": "Lap Report", "Status": "Pending"},
            {"Patient": "Khaya", "Document Type": "ID", "Status": "Approved"},
            ]
    
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

if "logged_in_role" not in st.session_state:
    st.session_state.logged_in_role = None

nav_bar()

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

if st.session_state.logged_in_role == "Patient":
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
        search_term = st.text_input("Search your documents")
        st.subheader("Requested Documents")
        documents = [
            {"Document Needed": "Proof of Residence", "Requested by": "Staff", "Status": "Pending"},
        ]
        st.dataframe(documents)


        st.subheader("Upload a document")
        upoloaded_file = st.file_uploader("Upload here", type= ["pdf"])

    with tab4:
        st.subheader("Recent Activity")
        activity = [
            {"Action": "Lab Report viewed", "By": "Dr. Smith", "Date": "2026-08-03"},
            {"Action": "ID Document uploaded", "By": "You", "Date": "2026-08-02"},
        ]
        st.dataframe(activity)

elif st.session_state.logged_in_role == "Staff":
    tab1, tab2, tab3 = st.tabs(["Overview", "Manage Patient Documents", "Reports"])

    with tab1:
        st.header(f"Welcome back, {st.session_state.logged_in_user}")
        st.info("3 Documents pending approval")
    with tab2:
        st.subheader("Patient Documents")
        search_term = st.text_input("Search by patient name, ID or Document type")


        documents_to_show = st.session_state.all_documents

        if search_term:
            new_docu = []
            
            for d in st.session_state.all_documents:
                if search_term in d["Patient"]:
                    new_docu.append(d)
            documents_to_show = new_docu

        for d in documents_to_show:
            col1, col2, col3, col4, col5 = st.columns(5)

            with col1:
                st.write(d["Patient"])
            with col2:
                st.write(d["Document Type"])
            with col3:
                st.write(d["Status"])
            with col4:
                if d["Status"] == "Pending":
                    if st.button("Approve", key= f"approve_{d['Patient']}"):
                        d["Status"] = "Approved"
                else:
                    st.write("__")
            with col5:
                if d["Status"] == "Pending":
                    if st.button("Reject", key= f"reject_{d['Patient']}"):
                        d["Status"] = "Rejected"
                else:
                    st.write("__")

    with tab3:
        st.subheader("Reports")
        st.metric("Total Documents", "24")
        st.metric("Pending Approvals", "3")


if "confirm_signout" not in st.session_state:
    st.session_state.confirm_signout = False

if st.button("Sign Out"):
    st.switch_page("pages/7_SignOut.py")

