import streamlit as st
from nav import nav_bar
from backend_api import(get_pending_documents_for_staff, get_staff_dashboard_stats, get_documents_for_staff,
                        review_document, get_patient_profile, get_patient_documents, get_document_requests_for_patient, 
                        get_recent_activity_for_patient, get_admin_overview, get_all_staff, get_all_patients,
                        register_staff, register_admin, get_pending_patients, update_patient_status, download_document, 
                        get_document_types, upload_document)
from pdf_export import make_pdf

    
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

if "logged_in_role" not in st.session_state:
    st.session_state.logged_in_role = None

if "upload_counter" not in st.session_state:
    st.session_state.upload_counter = 0

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
            my_document_rows = [
                {
                    "Document Type": d["type_name"],
                    "File": d["original_filename"],
                    "Status": d["status"],
                    "Uploaded At": d["uploaded_at"],
                    "Rejection Reason": d.get("rejection_reason") or "-"
                }
                for d in documents
            ]
            st.dataframe(my_document_rows)
            st.download_button(
                "Download as PDF", data= make_pdf("My Documents", my_document_rows),
                file_name= "my_document.pdf", mime= "application/pdf", key= "my_documents_pdf"
            )

        st.divider()
        st.subheader("Upload a document")
        upload_message = st.session_state.pop("upload_success", None)
        if upload_message:
            st.success(upload_message)

        types_ok, document_types = get_document_types()
        if not types_ok:
            st.error(document_types.get("error", "Could not load the document types."))
        elif not document_types:
            st.info("No document types are set up yet.")
        else:
            types_ids = {t["type_name"]: t["document_type_id"] for t in document_types}
            chosen_type = st.selectbox("Document type", list(types_ids.keys()))
            pdf_file = st.file_uploader(
                "Upload here (PDF only, max 10MB)", type= ["pdf"],
                key= f"patient_upload_{st.session_state.upload_counter}"
            )
            if st.button("Submit document", key= "submit_document"):
                if pdf_file is None:
                    st.error("Please choose a PDF file first.")
                else:
                    ok, result = upload_document(pdf_file.getvalue(), pdf_file.name, types_ids[chosen_type])
                    if ok:
                        st.session_state.upload_success = f"'{pdf_file.name}' was uploaded and is waiting for staff review."
                        st.session_state.upload_counter += 1
                        st.rerun()
                    else:
                        st.error(result.get("error", "Could not upload the document."))

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
    tab1, tab2, tab3, tab4 = st.tabs(["Overview", "Manage Patient Documents", "Patient Registrations", "Reports"])

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

#---------Staff must open the the document before they can approve/ reject it----------------
            for d in documents_to_show:
                doc_id = d["document_id"]
                doc_key = f"doc_bytes_{doc_id}"
                opened = doc_key in st.session_state

                with st.container(border= True):
                    st.write(f"**{d['first_name']} {d['last_name']}** - {d['type_name']}")
                    st.caption(f"{d['original_filename']} | uploaded {d['uploaded_at']}")

                    if not opened:
                        if st.button("Open Document", key= f"open_{doc_id}"):
                            ok, result = download_document(doc_id)
                            if ok:
                                st.session_state[doc_key] = result
                                st.rerun()
                            else:
                                st.error(result.get("error", "Could not open the document."))
                        st.caption("Open the document first. Approve and Reject unlock afterwards.")
                    else:
                        st.download_button(
                            "Download PDF to view it", data = st.session_state[doc_key],
                            file_name= d["original_filename"], mime= "application/pdf",
                            key = f"download_{doc_id}"
                        )
                    reason = st.text_input(
                        "Rejection reason", key= f"reason_{doc_id}",
                        placeholder= "Rejection reason (needed to reject)", disabled= not opened
                    )

                    col_approve, col_reject = st.columns(2)

                    with col_approve:
                        if st.button("Approve", key = f"approve_{doc_id}", disabled= not opened):
                            ok, result = review_document(doc_id, "APPROVED")
                            if ok:
                                st.session_state.pop(doc_key, None)
                                st.rerun()
                            else:
                                st.error(result.get("error", "Could not approve document."))

                    with col_reject:
                        if st.button("Reject", key= f"reject_{doc_id}", disabled= not opened):
                            if not reason.strip():
                                st.error("Please enter a rejection reason.")
                            else:
                                ok, result = review_document(doc_id, "REJECTED", reason.strip())
                                if ok:
                                    st.session_state.pop(doc_key, None)
                                    st.rerun()
                                else:
                                    st.error(result.get("error", "Could not reject document."))

    with tab3:
        st.subheader("Pending Patient Registrations")
        reg_ok, pending_patients = get_pending_patients()

        if not reg_ok:
            st.error(pending_patients.get("error", "Could not load  pending Registrations."))
        elif not pending_patients:
            st.info("No pending registrations for now.")
        else:
            for patient in pending_patients:
                pid = patient["patient_id"]
                with st.container(border= True):
                    st.write(f"**{patient['first_name']} {patient['last_name']}**")
                    st.caption(f"ID Number: {patient['id_number']} | Registered: {patient['created_at']}")

#------The profile is only fetched when the toggle is on so that the page does not call the backend for every patient on every click-----

                    if st.toggle("Show registration details", key= f"reg_details_{pid}"):
                        profile_ok, profile = get_patient_profile(pid)

                        if not profile_ok:
                            st.error(profile.get("error", "Could not the details."))
                        else:
                            details = [
                                ("Date of birth", profile.get("date_of_birth")),
                                ("Mobile Phone", profile.get("mobile_phone")),
                                ("Email", profile.get("email")),
                                ("Address", profile.get("address_line")),
                                ("City", profile.get("city")),
                                ("Province", profile.get("province")),
                                ("Medical Aid", profile.get("provider") if profile.get("has_medical_aid") else "None"),
                            ]
                            for label, value in details:
                                st.write(f"**{label}:** {value if value not in (None, '') else '--'}")
                    col_approve, col_reject = st.columns(2)
                    with col_approve:
                        if st.button("Approve", key= f"reg_approve_{pid}"):
                            done, result = update_patient_status(pid, "APPROVED")
                            if done:
                                st.rerun()
                            else:
                                st.error(result.get("error", "Could not approve this patient."))
                    with col_reject:
                        if st.button("Reject", key=f"reg_reject_{pid}"):
                            done, result = update_patient_status(pid, "REJECTED")
                            if done:
                                st.rerun()
                            else:
                                st.error(result.get("error", "Could not reject this patient."))

    with tab4:
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

#--------------------------PDF export for STAFF---------------------------------------------------------------------------

            summary_rows = [
                {"Metric": "Total Documents", "Value": stats["total_documents"]},
                {"Metric": "Pending Approvals", "Value": stats["pending_approvals"]},
                {"Metric": "Approved Documents", "Value": stats["approved_documents"]},
                {"Metric": "Rejected Documents", "Value": stats["rejected_documents"]},
                {"Metric": "Pending Patient Registrations", "Value": stats["pending_patient_registrations"]},
            ]
            st.download_button(
                "Download summary as PDF", data= make_pdf("Staff Summary Report", summary_rows),
                file_name= "staff_summary.pdf", mime= "application/pdf", key= "staff_summary_pdf"
            )

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
            st.download_button(
                "Download ppatients as PDF", data= make_pdf("All Patients", patients),
                file_name= "all_patients.pdf", mime= "application/pdf", key= "admin_patients_pdf"
            )

    with tab2:
        st.subheader("Add a staff member")
        #Shown once after a successful add because the username is generated by the backend
        created = st.session_state.pop("staff_created", None)
        if created:
            st.success(
                f"Staff member created. Username: {created['username']} | Staff number: {created['staffNumber']}. "
                "Give the username and the password you set to the staff member."
            )

        with st.form("add_staff_form", clear_on_submit= False, enter_to_submit= False):
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

        with st.form("add_admin_form", clear_on_submit= False, enter_to_submit= False):
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
            st.download_button(
                "Download Staff as PDF",
                data= make_pdf("Staff Members", staff_list,
                               ["first_name", "last_name", "staff_number", "job_title",
                                "department", "email", "created_at"
                                ]),
                file_name = "staff_members.pdf", mime= "application/pdf", key= "admin_staff_pdf"
                )
            
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
            st.download_button(
                "Download Report as PDF",
                data= make_pdf("Document Report", documents,
                               ["first_name", "last_name", "id_number", "type_name",
                                "original_filename", "status", "uploaded_at"
                                ]),
                file_name= "document_report.pdf", mime= "application/pdf", key= "admin_in_documents_pdf"
            )

    with tab4:
        st.subheader("Analytics")
        ok, overview = get_admin_overview()
        if not ok:
            st.error(overview.get("error", "Could not load analytics."))
        else:
            #Only real numbers from the backend
            chart_left, chart_right = st.columns(2)

            with chart_left:
                st.write("**Patients by status**")
                st.bar_chart({
                    "Status": ["Pending", "Approved", "Rejected"],
                    "Patients": [
                        overview["pending_patients"],
                        overview["approved_patients"],
                        overview["rejected_patients"],
                    ],
                }, x= "Status", y= "Patients")

            with chart_right:
                st.write("**Documents by status**")

#total_documents/ pending_documents come from overview. Approved and rejected come from the dashboard stats
                stats_ok, stats = get_staff_dashboard_stats()
                if not stats_ok:
                    st.error(stats.get("error", "Could not load documents stats."))
                else:
                    st.bar_chart({
                        "Status": ["Pending", "Approved", "Rejected"],
                        "Documents": [
                            stats["pending_approvals"],
                            stats["approved_documents"],
                            stats["rejected_documents"],
                        ],
                    }, x= "Status", y= "Documents")


if "confirm_signout" not in st.session_state:
    st.session_state.confirm_signout = False

if st.button("Sign Out"):
    st.switch_page("pages/7_SignOut.py")
