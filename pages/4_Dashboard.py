import streamlit as st
from nav import nav_bar
from db import get_recent_activity_for_patient
from backend_api import(get_pending_documents_for_staff, get_staff_dashboard_stats, get_documents_for_staff,
                        review_document, get_patient_profile, get_patient_documents, get_document_requests_for_patient,)

    
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
        requests_ok, document_requests = get_document_requests_for_patient(st.session_state.patient_id)

        if not requests_ok:
            st.error(document_requests.get("error", "Could not load document requests."))
        else:
            pending_requests = [r for r in document_requests if r["status"] == "PENDING"]
            if not pending_requests:
                st.success("No pending document requests.")
            else:
                needed = ", ".join(r["type_name"] for r in pending_requests)
                st.warning(
                    f"You have {len(pending_requests)} pending document request(s) - "
                    f"{needed}. See 'My Documents' tab."
                )
        
    with tab2:
        profile_ok, profile = get_patient_profile(st.session_state.patient_id)

        if not profile_ok:
            st.error(profile.get("error", "Could not load your profile."))
        else:
            def show_field(label, value):
                st.caption(f"**{label}**")
                st.write(value if value not in (None, "") else "--")

            show_field("Title", profile.get("title"))
            show_field("First Name", profile.get("first_name"))
            show_field("Middle Name", profile.get("middle_name"))
            show_field("Last Name", profile.get("last_name"))
            show_field("Preferred Name", profile.get("preferred_name"))
            show_field("Date of Birth", profile.get("date_of_birth"))
            show_field("ID Number", profile.get("id_number"))
            show_field("Address", profile.get("address_line"))
            show_field("City", profile.get("city"))
            show_field("Province", profile.get("province"))
            show_field("Postal Code", profile.get("postal_code"))
            show_field("Country", profile.get("country"))
            show_field("Mobile Phone", profile.get("mobile_phone"))
            show_field("Home Phone", profile.get("home_phone"))
            show_field("Email", profile.get("email"))

            if profile.get("has_medical_aid"):
                show_field("Medical Aid Provider", profile.get("provider"))
                show_field("Membership Number", profile.get("membership_number"))
            else:
                st.caption("**Medical Aid**")
                st.write("Not on medical aid")

        st.button(
            "REQUEST CHANGE", disabled= True,
           ####### help= "Coming soon"
        )

            
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
        activity = get_recent_activity_for_patient(st.session_state.patient_id)
        if activity:
            st.dataframe(activity)
        else:
            st.info("No activity yet.")

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

