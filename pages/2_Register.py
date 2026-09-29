import streamlit as st
from nav import nav_bar
from datetime import datetime
from backend_api import register_patient

nav_bar()

if "selected_role" not in st.session_state:
    st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

def birth_date_form_id(id_number):
    #Reading the YYYYMMDD on the SA ID number. 
    #It will return a date or nothing/ None
    #Picks the 2000s unless that would put the birth date in the future

    mm = int(id_number[2:4])
    dd = int(id_number [4:6])
    yy = int(id_number[0:2])
    today = date.today()
    for year in (2000 + yy, 1900 + yy):
        try:
            candidate = date(year, mm, dd):
        except ValueError:
            continue
        if candidate <= today:
            return candidate
    return None

title = st.selectbox("Select your title:",
                     options= ["Mr.", "Mrs.", "Miss.", "Dr.", "Prof."]
)

first_name = st.text_input("First Name")
middle_name = st.text_input("Middle Name")
last_name = st.text_input("Last Name")
nickname = st.text_input("Preferred name")

id_number = st.text_input("Enter your ID Number")
birth_date = None
birth_date_valid = False

if id_number:
    if len(id_number) != 13:
        st.error("ID Number must be 13 digits")
    elif not id_number.isdigit():
        st.error("ID Number must only contain numbers.")
    else:
        birth = birth_date_form_id(id_number)
        if birth is None:
            st.error("Invalid ID Number, the date of birth part (first 6 digits) is not a real date.")
        else:
            birth_date = birth.isoformat()
            birth_date_valid = True
            st.text_input("Date Of Birth", value= birth_date, disabled= True)

st.write("**Address**")

country = st.selectbox("Country",
                       options= ["South Africa"]
                       )
address = st.text_input("Enter your Address")
city = st.text_input("City")
postal = st.text_input("Enter your zip code")
province = st.selectbox("Province", 
                        options= ["Gauteng", "Limpopo",
                                  "Mpumalanga", "KwaZulu Natal",
                                  "Eastern Cape", "Western Cape",
                                  "Northern Cape", "Free State",
                                  "North West"
                                  ]
                        )
st.write("**Phone Numbers**")
home = st.text_input("Home")
if home:
    if len(home) != 10:
        st.error("Home Number must be 10 digit.")
    elif not home.isdigit():
        st.error("ID Number must only contain numbers.")
work = st.text_input("Work")
if work:
    if len(work) != 10:
        st.error("Work Number must only 10 digit.")
    elif not work.isdigit():
        st.error("ID Number must only contain numbers.") 

phone = st.text_input("Mobile Phone")
phone_2 = st.text_input("Secondary Phone")
if phone:
    if len(phone) != 10:
        st.error("Mobile Phone must only 10 digit.")
    elif not phone.isdigit():
        st.error("Mobile Phone must only contain numbers.") 
if phone_2:
    if len(phone_2) !=10:
        st.error("Secondary Phone must only be 10 digit.")
    elif not phone_2.isdigit():
        st.error("Secondary Phone must only contain numbers.")

st.write("**Email**")
email = st.text_input("Enter your email address")
email_2 = st.text_input("Enter your Secondary email address")

st.write("Medical - Aid")
medical_aid = st.radio("Do you have a Medical Aid ",
                       options= ["Yes", "No"]
                       )
provider = ""
membership_num = ""
if medical_aid == "Yes":
    provider = st.text_input("Provider")
    membership_num = st.text_input("Membership Number")

st.write("**Your Login details**")
username = st.text_input("Enter your Username")
password = st.text_input("Enter your Password", type= "password")
password_2 = st.text_input("Confirm password", type= "password")

required = [("First Name", first_name), ("Last Name", last_name),
            ("Preferred name", nickname), ("ID Number", id_number),
            ("Enter your Address", address), ("City", city),
            ("Enter your zip code", postal), ("Province", province),
            ("Mobile Phone", phone), ("Enter your email address", email),
            ("Enter yor Username", username), ("Enter your password", password)
            ]

missing_field = []
for field, value in required:
    if value is None or str(value).strip() == "":
        missing_field.append(field)

submit = st.button("Submit")
if submit:
    if missing_field:
        st.error(f"Please fill in the following fields: {', '.join(missing_field)}")
    elif not birth_date_valid:
        st.error("Please enter a valid ID Number before submitting.")
    elif len(password) <= 5:
        st.error("Password must be more than 5 characters")
    elif password != password_2:
        st.error("Password doesn't match")
    elif medical_aid == "Yes" and (not provider or not membership_num):
        st.error("Please fill in your Medical Aid provider and membership number or select 'No'.")
    else:
        with st.spinner("Creating your Account..."):
            success, result = register_patient(
                username = username,
                password = password,
                title = title,
                first_name = first_name,
                middle_name = middle_name,
                last_name = last_name,
                preferred_name = nickname,
                id_number = id_number,
                date_of_birth = birth_date,
                country = country,
                address_line = address,
                city = city,
                postal_code = postal,
                province = province,
                home_phone = home,
                work_phone = work,
                mobile_phone = phone,
                secondary_phone = phone_2,
                email = email,
                secondary_email = email_2,
                has_medical_aid = (medical_aid == "Yes"),
                provider = provider if medical_aid == "Yes" else None,
                membership_number = membership_num if medical_aid == "Yes" else None,
            )
        if success:
            #st.success() vanish as soon the pages switch, so creating so that it shows to the login page
            st.session_state.message = ("Account crreated. Your registration must be approved by Staff before you can log in.")
            st.switch_page("pages/1_Login.py")
        else:
            error = result.get("error", "Registration failed.")
            st.error(error)
            if error == "A database error occurred":
                st.caption("That username or ID Number may already be registered.")
