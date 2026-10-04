import streamlit as st
from nav import nav_bar
from backend_api import(get_pending_documents_for_staff, get_staff_dashboard_stats, get_documents_for_staff,
                        review_document, get_patient_profile, get_patient_documents, get_document_requests_for_patient, 
                        get_recent_activity_for_patient, get_admin_overview, get_all_staff, get_all_patients,
                        register_staff, register_admin, get_pending_patients, update_patient_status, download_document, 
                        get_document_types, upload_document, search_patients, request_document, submit_profile_change,
                        get_profile_changes_for_patient, get_pending_profile_changes, resolve_profile_change)
from pdf_export import make_pdf, make_patient_confirmation, make_staff_confirmation
from formatting import fmt_datetime, fmt_date, format_rows

#the profile details the backendlets a patient change except names& ID 
PROFILE_FIELD_LABELS = {
    "address_line": "Address",
    "city": "City",
    "province": "Province",
    "postal_code": "Postal Code",
    "home_phone": "Home Phone",
    "work_phone": "Work Phone",
    "mobile_phone": "Mobile Phone",
    "secondary_phone": "Secondary Phone",
    "email": "Email",
    "secondary_email": "Secondary email",
    "provider": "Medical Aid Provider",
    "membership_number": "Medical Aid Membership Number",
}
MEDICAL_AID_FIELDS = ("provider", "membership_number")

    
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

if "logged_in_role" not in st.session_state:
    st.session_state.logged_in_role = None

if "upload_counter" not in st.session_state:
    st.session_state.upload_counter = 0

#Emptying the request a change box after a successful request
if "change_counter" not in st.session_state:
    st.session_state.change_counter = 0

nav_bar()

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

if st.session_state.logged_in_role == "Patient":
    tab1, tab2, tab3, tab4 = st.tabs(["Overview", "My profile", "My Documents", "Recent Activity"])


    with tab1:
        st.header(f"Welcome back, {st.session_state.logged_in_user}")
    #POR 
        my_profile_ok, my_profile = get_patient_profile(st.session_state.patient_id)
        activity_ok, my_activity = get_recent_activity_for_patient(st.session_state.patient_id)
        approval = None
        if activity_ok:
            approval = next((a for a in my_activity if a.get("Action") == "Registration approved"), None)

        if my_profile_ok:
            st.success("Your registration with SyncPoint is confirmed. You are a registered patient.")
            with st.container(border= True):
                st.write("**Proof of Registration**")
                st.caption(
                    f"Registration date: {fmt_date(approval.get('Date')) if approval else '-'} |  "
                    f"Approved by: {approval.get('By') if approval else '-'}"
                )
                st.download_button(
                    "Download proof of registration (PDF)",
                    data= make_patient_confirmation(
                        st.session_state.patient_id, my_profile,
                        approval.get("Date") if approval else None,
                        approval.get("By") if approval else None,
                    ),
                    file_name= "SyncPoint_proof_of_registration.pdf", mime= "application/pdf",
                    key= "patient_confirmation_pdf",
                    help= "The PDF can be printed. Editing and copying are switched off."
                )

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

        #Request a change-------
            st.divider()
            st.subheader("Request a change")
            st.caption("Staff will review your request. Your ID Number and your names cannot be changed.")

            change_message = st.session_state.pop("change_success", None)
            if change_message:
                st.success(change_message)

            available_fields = {
                label: field for field, label in PROFILE_FIELD_LABELS.items()
                if field not in MEDICAL_AID_FIELDS or profile.get("has_medical_aid")
            } 
            chosen_label = st.selectbox("What do you want to change?", list(available_fields.keys()), key= "change_field")
            chosen_field = available_fields[chosen_label]
            current_value = profile.get(chosen_field)
            st.caption(f"Current value: {current_value if current_value not in (None, '') else '--'}")

            new_value = st.text_input("New value", key= f"change_value_{st.session_state.change_counter}")
            change_reason = st.text_area("Reason (optional)", key= f"change_reason_{st.session_state.change_counter}")

            if st.button("Send change request", key= "send_change_request"):
                if not new_value.strip():
                    st.error("Please enter the new value.")
                else:
                    ok, result = submit_profile_change(chosen_field, new_value.strip(), change_reason)
                    if ok:
                        st.session_state.change_success = "Your request was sent. You can follow it in the table below."
                        st.session_state.change_counter += 1
                        st.rerun()
                    else:
                        st.error(result.get("error", "Could not send the request."))

        st.divider()
        st.subheader("My change requests")
        change_ok, my_changes = get_profile_changes_for_patient(st.session_state.patient_id)
        if not change_ok:
            st.error(my_changes.get("error", "Could not load your change requests."))
        elif not my_changes:
            st.info("You have not asked for any changes.")
        else:
            st.dataframe(format_rows([
                {
                    "Detail": PROFILE_FIELD_LABELS.get(c["field_name"], c["field_name"]),
                    "Old Value": c.get("old_value") or "-",
                    "Requested Value": c["requested_value"],
                    "Status": c["status"],
                    "Requested At": c["requested_at"],
                    "Reviewed At": c.get("reviewed_at"),
                }
                for c in my_changes
            ]))

    with tab3:
        st.subheader("Requested Documents")
        requests_ok, document_requests = get_document_requests_for_patient(st.session_state.patient_id)

        if not requests_ok:
            st.error(document_requests.get("error", "Could not load document requests."))
        elif not document_requests:
            st.info("No document requests from staff right now.")
        else:
            st.dataframe(format_rows([
                {
                    "Document Needed": r["type_name"],
                    "Reason": r.get("request_reason") or "-",
                    "Status": r["status"],
                    "Requested At": r["requested_at"],
                }
                for r in document_requests
            ]))

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
            st.dataframe(format_rows(my_document_rows))
            st.download_button(
                "Download as PDF", data= make_pdf("My Documents", my_document_rows),
                file_name= "my_documents.pdf", mime= "application/pdf", key= "my_documents_pdf"
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
            st.dataframe(format_rows(activity))

elif st.session_state.logged_in_role == "Staff":
    tab1, tab2, tab3, tab4, tab5, tab6 = st.tabs([
        "Overview", "Manage Patient Documents", "Patient Registrations", 
        "Request a Document", "Profile Changes" ,"Reports"
    ])

    with tab1:
        st.header(f"Welcome back, {st.session_state.logged_in_user}")
        stats_ok, stats = get_staff_dashboard_stats()
        if not stats_ok:
            st.error(stats.get("error", "Could not load dashboard stats."))
        else:
        #Making Documents and patient registration waiting for review
            col1, col2 = st.columns(2)

            with col1:
                st.metric("Documents waiting for review", stats["pending_approvals"])
            with col2:
                st.metric("Patient registrations waiting", stats["pending_patient_registrations"])
                st.caption("Review them in the 'Manage Patient Documents' and 'Patient Registrations' tabs.")
                
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
                    st.caption(f"{d['original_filename']} | uploaded {fmt_datetime(d['uploaded_at'])}")

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
            st.error(pending_patients.get("error", "Could not load pending Registrations."))
        elif not pending_patients:
            st.info("No pending registrations for now.")
        else:
            for patient in pending_patients:
                pid = patient["patient_id"]
                with st.container(border= True):
                    st.write(f"**{patient['first_name']} {patient['last_name']}**")
                    st.caption(f"ID Number: {patient['id_number']} | Registered: {fmt_datetime(patient['created_at'])}")


#------The profile is only fetched when the toggle is on so that the page does not call the backend for every patient on every click-----

                    if st.toggle("Show registration details", key= f"reg_details_{pid}"):
                        profile_ok, profile = get_patient_profile(pid)

                        if not profile_ok:
                            st.error(profile.get("error", "Could not load the details."))
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

#New tab Staff asks a patient for a document
    with tab4:
        st.subheader("Request a document from a patient")
        st.caption("The patient sees the request on their Overview and in 'My Documents'.")

        request_message = st.session_state.pop("doc_request_success", None)
        if request_message:
            st.success(request_message)

        patient_term = st.text_input("Patient name or ID Number (at least 2 characters)", key= "request_patient_term")

#The search runs only when the button is pressed

        if st.button("Search patients", key= "request_patient_search"):
            found_ok, found = search_patients(patient_term)
            if found_ok:
                st.session_state.request_patient_results = found
            else:
                st.session_state.request_patient_results = []
                st.error(found.get("error", "Could not search for patients."))

        found_patients = st.session_state.get("request_patient_results", [])
        if found_patients:
            patient_options = {
                f"{p['first_name']} {p['last_name']} - ID {p['id_number']}": p["patient_id"]
                for p in found_patients
            }
            chosen_patient = st.selectbox("Patient", list(patient_options.keys()), key= "request_patient_choice")

            types_ok, request_types = get_document_types()
            if not types_ok:
                st.error(request_types.get("error", "Could not load the document types."))
            else:
                request_type_ids = {t["type_name"]: t["document_type_id"] for t in request_types}
                chosen_request_type = st.selectbox("Document needed", list(request_type_ids.keys()), key= "request_type_choice")
                request_reason = st.text_area("Reason (optional)", key= "request_reason")

                if st.button("Send request", key= "send_document_request"):
                    ok, result = request_document(
                        patient_options[chosen_patient], request_type_ids[chosen_request_type], request_reason
                    )
                    if ok:
                        st.session_state.doc_request_success = f"Request for '{chosen_request_type}' sent to {chosen_patient.split(' - ')[0]}."
                        st.session_state.request_patient_results = []
                        st.rerun()
                    else:
                        st.error(result.get("error", "Could not send the request."))
        elif st.session_state.get("request_patient_results") == [] and patient_term:
            st.caption("No approved patient found. Only approved can be founded.")

#TAB5 Staff approve/ reject the profile changes

    with tab5:
        st.subheader("Profile change requests")
        change_ok, pending_changes = get_pending_profile_changes()

        if not change_ok:
            st.error(pending_changes.get("error", "Could not load the change requests."))
        elif not pending_changes:
            st.info("No profile change requests right now.")
        else:
            for change in pending_changes:
                crd = change["change_request_id"]
                with st.container(border= True):
                    st.write(f"**{change['first_name']} {change['last_name']}** wants to change "
                             f"**{PROFILE_FIELD_LABELS.get(change['field_name'], change['field_name'])}**")
                    st.write(f"From: {change.get('old_value') or '--'}")
                    st.write(f"To: **{change['requested_value']}**")
                    if change.get("reason"):
                        st.caption(f"Reason: {change['reason']}")
                    st.caption(f"Requested: {fmt_datetime(change['requested_at'])}")

                    col_approve, col_reject = st.columns(2)

                    with col_approve:
                        if st.button("Approve", key= f"change_approve_{crd}"):
                            done, result = resolve_profile_change(crd, True)
                            if done:
                                st.rerun()
                            else:
                                st.error(result.get("error", "Could not approve this request."))
                    with col_reject:
                        if st.button("Reject", key= f"change_reject_{crd}"):
                            done, result = resolve_profile_change(crd, False)
                            if done:
                                st.rerun()
                            else:
                                st.error(result.get("error", "Could not reject this request."))
    with tab6:
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
        #seeing who approv/rejected a registration
            staff_look_ok, staff_look = get_all_staff()
            staff_names = {}
            if staff_look_ok:
                staff_names = {
                    s_row["staff_id"]: f"{s_row['first_name']} {s_row['last_name']} ({s_row['job_title']})"
                    for s_row in staff_look
                }
            patient_rows = []
            for patient in patients:
                row = dict(patient)
                reviewer_id = row.pop("reviewed_by_staff_id", None)
                row["reviewed_by"] = staff_names.get(reviewer_id, "-") if reviewer_id else "-"
                patient_rows.append(row)
            patient_columns = ["patient_id", "first_name", "last_name", "id_number", "status",
                               "reviewed_by", "reviewed_at", "created_at"]

            st.dataframe(format_rows(patient_rows), column_order= patient_columns)
            st.download_button(
                "Download patients as PDF", data= make_pdf("All Patients", patient_rows, patient_columns),
                file_name= "all_patients.pdf", mime= "application/pdf", key= "admin_patient_pdf"
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
                email = st.text_input("Email*")
                password = st.text_input("Initial password*", type= "password")
            with col2:
                last_name = st.text_input("Last Name*")
                courtesy_title = st.text_input("Courtesy Title (e.g. Dr, Mr, Ms)")
                specialization = st.text_input("Specialization")
                confirm_password = st.text_input("Confirm password*", type= "password")

            submitted = st.form_submit_button("Add Staff Member")

        if submitted:
            if not first_name.strip() or not last_name.strip() or not email.strip() or not password:
                st.error("First name, last name, email and the password are required.")
            elif "@" not in email or "." not in email.split("@")[-1]:
                st.error("Please enter a valid email address.")
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
                    st.error(result.get("error", "Could not add the staff member."))

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
            st.info("No Staff members yet.")
        else:
            st.dataframe(format_rows(staff_list))
        #Confirmation PDF for one staff member
            st.write("**Staff Confirmation**")
            staff_labels = [
                f"{m['first_name']} {m['last_name']} - {m['staff_number']} ({m['job_title']})" for m in staff_list
            ]
            chosen_index = st.selectbox(
                "Staff Member", range(len(staff_list)), format_func= lambda i: staff_labels[i],
                key= "confirmation_staff_choice"
            )
            st.download_button(
                "Download Staff confirmation (PDF)",
                data= make_staff_confirmation(staff_list[chosen_index]),
                file_name= f"SyncPoint_staff_confirmation_{staff_list[chosen_index]['staff_number']}.pdf",
                mime= "application/pdf", key= "staff_confirmation_pdf",
                help= "The PDF can be printed. Editing and copying are switched off."
            )
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
        search_term = st.text_input("Search by patient name, ID, or document type", key= "admin_doc_search")
        success, documents = get_documents_for_staff(search_term)

        if not success:
            st.error(documents.get("error", "Could not load documents."))
        elif not documents:
            st.info("No documents found.")
        else:
        #backend sends who reviewed each document
            report_rows = []
            for doc in documents:
                row = dict(doc)
                reviewer = row.get("reviewed_by_name")
                if reviewer and row.get("reviewed_by_staff_number"):
                    reviewer = f"{reviewer} ({row['reviewed_by_staff_number']})"
                row["reviewed_by"] = reviewer or "-"
                row["rejection_reason"] = row.get("rejection_reason") or "-"
                report_rows.append(row)

            report_columns = ["first_name", "last_name", "id_number", "type_name", 
                              "original_filename", "status", "uploaded_at", "reviewed_by", "reviewed_at", "rejection_reason"]
            st.dataframe(format_rows(report_rows), column_order= report_columns)

            st.download_button(
                "Download Report as PDF",
                data= make_pdf("Document Report", report_rows,
                               ["first_name", "last_name", "id_number", "type_name",
                                "status", "uploaded_at", "reviewed_by", "reviewed_at", "rejection_reason"
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
    st.switch_page("pages/5_SignOut.py")
