import base64
import streamlit as st
from nav import nav_bar

nav_bar()
st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

st.title("Contact Sync Point")
tab1, tab2, tab3 = st.tabs(["About SyncPoint", "FAQ", "Quick Guide"])

with tab1:
    st.image("assets/logo-removebg.png",width= 200 )
    st.write("""
    SyncPoint is a Hospital Digital Archive System designed to securely store, organise, and retrieve hospital documents digitally.
    Us at SyncPoint we help to reduce reliance on paper based records while making documents easier to find and access.
    It allows authorised users to manage and access patient related documents while helping protect records from being lost or damaged.
    """)
    st.divider()
    st.subheader("Who is SyncPoint For")
    st.write("**For Patients**")
    st.write("Access to their own archived Documents")
    st.write("Search their Documents")
    st.write("View and download available Documents")
    st.write("**Staff**")
    st.write("Upload and archive documents")
    st.write("Categorise documents")
    st.write("Search and retrieve patients documents")
    st.write("Manage archived documents according to their permissions")
    st.divider()
    st.subheader("How SyncPoint Works")
    gif_path = "assets/Video Project 2.gif"

    with open(gif_path, "rb") as file:
        gif_data = base64.b64encode(file.read()).decode()

    st.markdown(
        f"""
        <div style="text-align: center;">
           <img src="data:image/gif;base64,{gif_data}" width="1500">
        </div>
        """,
        unsafe_allow_html=True
    ) 

    