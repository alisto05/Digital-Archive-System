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
len(id_number)
if id_number:
    if len(id_number) != 13:
        st.error("ID Number must be 13 digits")
    else:
        mm = int(id_number[2:4])
        dd = int(id_number [4:6])
        yy = int(id_number[0:2])
        year_option_1 = 1900 + yy
        year_option_2 = 2000 + yy
        current_year = datetime.now().year
        valid_years = []
        valid_years.append(year_option_1)

        if year_option_2 < current_year:
            valid_years.append(year_option_2)
        selected_year = st.selectbox("Select your Birth Year",
                    options= valid_years
                    )
            
        if mm < 1 or mm > 12:
            st.error("Invalid ID Number, Check the month section.")
        elif dd < 1 or dd > 31:
            st.error("Invalid ID Number, Check the date section.")

