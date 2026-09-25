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

    with tab3:
        st.subheader("Requested Documents")
        requests_ok, document_requests = get_document_requests_for_patient(st.session_state.patient_id)

        if not requests_ok:
            st.error(document_requests.get("error", "Could not load document requests."))
        elif not document_requests:
            st.info("No document requests from staff right now.")
        else:
            st.dataframe([
                {
                    "Document Needed": r["type_name"],
                    "Reason": r.get("request_reason") or "-",
                    "Status": r["status"],
                    "Requested At": r["requested_at"],
                }
                for r in document_requests
            ])

        st.divider()
        st.subheader("Your Uploaded Documents")
        search_term = st.text_input("Search your documents")
        docs_ok, documents = get_patient_documents(st.session_state.patient_id, search_term)

        if not docs_ok:
            st.error(documents.get("error", "Could not load your documents."))
        elif not documents:
            st.info("You haven't uploaded any documents yet.")
        else:
            st.dataframe([
                {
                    "Document Type": d["type_name"],
                    "File": d["original_filename"],
                    "Status": d["status"],
                    "Uploaded At": d["uploaded_at"],
                    "Rejection Reason": d.get("rejection_reason") or "-"
                }
                for d in documents
            ])

        st.divider()
        st.subheader("Upload a document")
        st.file_uploader(
            "Upload here", type= ["pdf"], disabled= True,
            help= "Coming soon"
        )

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
        stats_ok, stats = get_staff_dashboard_stats()
        if not stats_ok:
            st.error(stats.get("error", "Could not load dashboard stats."))
        else:
            st.info(f"{stats['pending_approval']} Documents pending approval")

    with tab2:
        st.subheader("Patient Documents")
        search_term = st.text_input("Search by patient name, ID or Document type")

        docs_ok, pending_documents = get_pending_documents_for_staff()

        if not docs_ok:
            st.error(pending_documents.get("error", "Could not load pending documents."))

        else:
            documents_to_show = pending_documents

            if search_term:
                search_lower = search_term.lower()
                documents_to_show = [
                    d for d in pending_documents
                    if search_lower in f"{d['first_name']} {d['last_name']}".lower()
                    or search_lower in d["type_name"].lower()
                ]

            if not documents_to_show:
                st.info("No pending documents right now.")

            for d in documents_to_show:
                col1, col2, col3, col4, col5 = st.columns(5)

                with col1:
                    st.write(f"{d['first_name']} {d['last_name']}")
                with col2:
                    st.write(d["type_name"])
                with col3:
                    st.write(d["original_filename"])
                with col4:
                    if st.button("Approve", key= f"approve_{d['document_id']}"):
                        ok, result = review_document(d["document_id"], "APPROVED", st.session_state.user_id)
                        if ok:
                            st.rerun()
                        else:
                            st.error(result.get("error", "Could not approve document."))

                with col5:
                    if st.button("Reject", key= f"reject_{d['document_id']}"):
                        ok, result = review_document(d["document_id"], "REJECTED", st.session_state.user_id)
                        if ok:
                            st.rerun()
                        else:
                            st.error(result.get("error", "Could not reject document."))

    with tab3:
        st.subheader("Reports")
        stats_ok, stats = get_staff_dashboard_stats()
        if not stats_ok:
            st.error(stats.get("error", "Could not load report stats."))
        else:
            col1, col2, col3 = st.columns(3)
            with col1:
                st.metric("Total Documents", stats["total_documents"])
            with col2:
                st.metric("Pending Approvals", stats["pending_approvals"])
            with col3:
                st.metric("Pending Patient Registrations", stats["pending_patient_registrations"])

            col4, col5 = st.columns(2)
            with col4:
                st.metric("Approved Documents", stats["approved_documents"])
            with col5:
                st.metric("Rejected Documents", stats["rejected_documents"])


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

