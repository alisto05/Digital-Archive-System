import streamlit as st
from nav import nav_bar

nav_bar()
st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

title = st.selectbox("Select your title:",
                     options= ["Mr.", "Mrs.", "Miss.", "Dr.", "Prof."]
)

first_name = st.text_input("First Name")
middle_name = st.text_input("Middle Name")
laste_name = st.text_input("Last Name")

id_number = st.text_input("Enter your ID Number")
len(id_number)
if len(id_number) != 13:
    st.error("ID Number must be 13 digits")

