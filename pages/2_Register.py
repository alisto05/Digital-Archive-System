import streamlit as st
from nav import nav_bar
from datetime import datetime

nav_bar()
st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

title = st.selectbox("Select your title:",
                     options= ["Mr.", "Mrs.", "Miss.", "Dr.", "Prof."]
)

first_name = st.text_input("First Name")
middle_name = st.text_input("Middle Name")
last_name = st.text_input("Last Name")

id_number = st.text_input("Enter your ID Number")

if id_number:
    if len(id_number) != 13:
        st.error("ID Number must be 13 digits")
    elif not id_number.isdigit():
        st.error("ID Number must only contain numbers.")
    else:

        #Extracting DOB infor from the ID Number
        mm = int(id_number[2:4])
        dd = int(id_number [4:6])
        yy = int(id_number[0:2])

        #Determing the possible year
        year_option_1 = 1900 + yy
        year_option_2 = 2000 + yy

        current_year = datetime.now().year
        if year_option_2 < current_year:
            selected_year = year_option_2
        else:
            selected_year = year_option_1

        #Checking if the ID Number is correcting using ID Number 
        if mm < 1 or mm > 12:
            st.error("Invalid ID Number, Check the month section.")
        elif dd < 1 or dd > 31:
            st.error("Invalid ID Number, Check the date section.")
        else:

            #After giving the correct date of birth
            birth_date = f"{selected_year}-{mm:02d}-{dd:02d}"
            st.selectbox(
                "Date of Birth",
                options= [birth_date]
            )
            