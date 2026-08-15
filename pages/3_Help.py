import streamlit as st
from nav import nav_bar

nav_bar()
if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

st.title("Contact Sync Point")
tab1, tab2, tab3 = st.tabs(["About SyncPoint", "FAQ", "Quick Guide"])

with tab1:
    st.image("assets/logo-removebg.png", width=200)
    st.write("""
    SyncPoint is a Hospital Digital Archive System designed to securely store, organise, and retrieve hospital documents digitally.
    We at SyncPoint help reduce reliance on paper-based records while making documents easier to find and access.
    It allows authorised users to manage and access patient-related documents while helping protect records from being lost or damaged.
    """)

    st.divider()
    st.subheader("Who is SyncPoint For")

    st.markdown("**For Patients**")
    st.markdown("""
    - Access their own archived documents
    - Search their documents
    - View and download available documents
    """)

    st.markdown("**For Staff**")
    st.markdown("""
    - Upload and archive documents
    - Categorise documents
    - Search and retrieve patient documents
    - Manage archived documents according to their permissions
    """)

    st.divider()
    st.subheader("How SyncPoint Works")

    st.image("assets/Video Project 2.gif", use_container_width=True)

with tab2:
    with st.expander("How do I Register?"):
        st.write("Click on **Register** in the navigation bar and fill in your details to create an account.")

    with st.expander("How do I search for a document?"):
        st.write("Once you've registered and logged in, go to the **Search** page to look up your documents.")

    with st.expander("Can patients see other patients' documents?"):
        st.write("No. Patients can only view and search their own archived documents. Access to other patients' records is restricted to authorised Staff.")

    with st.expander("What should I do if I can't log in?"):
        st.write("Double check your username and password are entered correctly. If you're still unable to log in, contact SyncPoint support for assistance.")
        # No password-reset feature exists yet.

with tab3:
    # After the full system is built/done
    st.subheader("A Full Quick Guide")
    st.image("assets/Quick-guide.png", use_container_width=True)

st.divider()