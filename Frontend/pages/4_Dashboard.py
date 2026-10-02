import streamlit as st
from nav import nav_bar
from backend_api import(get_pending_documents_for_staff, get_staff_dashboard_stats, get_documents_for_staff,
                        review_document, get_patient_profile, get_patient_documents, get_document_requests_for_patient, 
                        get_recent_activity_for_patient, get_admin_overview, get_all_staff, get_all_patients, register_staff, register_admin)

    
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
        ok, activity = get_recent_activity_for_patient(st.session_state.patient_id)
        if not ok:
            st.error(activity.get("error", "Could not load your activity."))
        elif not activity:
            st.info("No activity yet.")
        else:
            st.dataframe(activity)

elif st.session_state.logged_in_role == "Staff":
    tab1, tab2, tab3 = st.tabs(["Overview", "Manage Patient Documents", "Reports"])

    with tab1:
        st.header(f"Welcome back, {st.session_state.logged_in_user}")
        stats_ok, stats = get_staff_dashboard_stats()
        if not stats_ok:
            st.error(stats.get("error", "Could not load dashboard stats."))
        else:
            st.metric("Pending Approvals", stats["pending_approvals"])

    with tab2:
        st.subheader("Patient Documents")
        search_term = st.text_input("Search by patient name or Document type")

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
                        ok, result = review_document(d["document_id"], "APPROVED")
                        if ok:
                            st.rerun()
                        else:
                            st.error(result.get("error", "Could not approve document."))

                with col5:
                    reason = st.text_input("Reason", key= f"reason_{d['document_id']}", label_visibility= "collapsed", placeholder= "Rejection reason")
                    if st.button("Reject", key= f"reject_{d['document_id']}"):
                        if not reason.strip():
                            st.error("Please enter a rejection reason.")
                        else:
                            ok, result = review_document(d["document_id"], "REJECTED", reason.strip())
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

elif st.session_state.logged_in_role == "Admin":
    tab1, tab2, tab3, tab4 = st.tabs(["Overview", "Manage Staff", "Document Reports", "Analytics"])

    with tab1:
        st.header(f"Welcome back, {st.session_state.logged_in_user}")
        ok, overview = get_admin_overview()
        if not ok:
            st.error(overview.get("error", "Could not load the overview."))
        else:
            col1, col2, col3, col4 = st.columns(4)

            with col1:
                st.metric("Patients", overview["total_patients"])
            with col2:
                st.metric("Staff", overview["total_staff"])
            with col3:
                st.metric("Admins", overview["total_admins"])
            with col4:
                st.metric("Documents", overview["total_documents"])

            col5, col6 = st.columns(2)

            with col5:
                st.metric("Pending patient Registrations", overview["pending_patients"])
            with col6:
                st.metric("Documents waiting for review", overview["pending_documents"])

        st.divider()
        st.subheader("All patients")
        ok, patients = get_all_patients()

        if not ok:
            st.error(patients.get("error", "Could not load patients."))
        elif not patients:
            st.info("No patients have registered yet.")
        else:
            st.dataframe(patients)

    with tab2:
        st.subheader("Add a staff member")
        #Shown once after a successful add because the username is generated by the backend
        created = st.session_state.pop("staff_created", None)
        if created:
            st.success(
                f"Staff member created. Username: {created['username']} | Staff number: {created['staffNumber']}. "
                "Give the username and the password you set to the staff member."
            )

        with st.form("add_staff_form", clear_on_submit= True):
            col1, col2 = st.columns(2)

            with col1:
                first_name = st.text_input("First Name*")
                job_title = st.selectbox("Job Title*", ["Doctor", "Nurse", "Receptionist"])
                department = st.text_input("Department")
                email = st.text_input("Email")
                password = st.text_input("Initial password*", type= "password")
            with col2:
                last_name = st.text_input("Last Name*")
                courtesy_title = st.text_input("Courtesy Title (e.g. Dr, Mr, Ms)")
                specialization = st.text_input("Specialization")
                confirm_password = st.text_input("Confirm password*", type= "password")

            submitted = st.form_submit_button("Add Staff Member")

        if submitted:
            if not first_name.strip() or not last_name.strip() or not password:
                st.error("First name, last name and the password are required.")
            elif password != confirm_password:
                st.error("The two passwords do not match.")
            else:
                ok, result = register_staff(
                    password, first_name, last_name, job_title, courtesy_title, 
                    department, email, specialization
                )
                if ok:
                    st.session_state.staff_created = result
                    st.rerun()
                else:
                    st.error(result.get("error", "Could not add the staff Member."))

        st.divider()

        #Adding a new admin

        st.subheader("Add an Admin")
        admin_created = st.session_state.pop("admin_created", None)
        if admin_created:
            st.success(f"Admin created. They can log in now as the admin with username: {admin_created}")

        with st.form("add_admin_form", clear_on_submit= True):
            col1, col2 = st.columns(2)

            with col1:
                admin_first = st.text_input("First Name*", key= "admin_first")
                admin_username = st.text_input("Username*", key= "admin_username")
                admin_password = st.text_input("Password*", type= "password", key= "admin_password")

            with col2:
                admin_last = st.text_input("Last Name*", key= "admin_last")
                st.caption("Choose the username yourself. It must not be 'admin' and must not already be taken.")
                admin_confirm = st.text_input("Confirm password*", type= "password", key= "admin_confirm")

            admin_submitted = st.form_submit_button("Add Admin")

        if admin_submitted:
            if not admin_first.strip() or not admin_last.strip() or not admin_username.strip() or not admin_password:
                st.error("First name, last name, username and password are all required.")
            elif admin_username.strip().lower() == "admin":
                st.error("Please choose a username other than 'admin'.")
            elif admin_password != admin_confirm:
                st.error("The two passwords do not match.")
            else:
                ok, result = register_admin(admin_username, admin_password, admin_first, admin_last)
                if ok:
                    st.session_state.admin_created = admin_username.strip()
                    st.rerun()
                else:
                    st.error(result.get("error", "Could not add the admin."))

        st.divider()
        st.subheader("Staff Members")
        ok, staff_list = get_all_staff()
        if not ok:
            st.error(staff_list.get("error", "Could not load staff."))
        elif not staff_list:
            st.info("No Staff Members yet.")
        else:
            st.dataframe(staff_list)
            
    with tab3:
        st.subheader("Document Reports")
        search_term = st.text_input("Search by patient name, ID, or doument type", key= "admin_doc_search")
        success, documents = get_documents_for_staff(search_term)

        if not success:
            st.error(documents.get("error", "Could not load documents."))
        elif not documents:
            st.info("No documents found.")
        else:
            st.dataframe(documents)

    with tab4:
        st.info(
                "Analytical charts coming"
        )

if "confirm_signout" not in st.session_state:
    st.session_state.confirm_signout = False

if st.button("Sign Out"):
    st.switch_page("pages/7_SignOut.py")
