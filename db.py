#Using my own Local Host first

import hashlib
import secrets
import mysql.connector
from mysql.connector import errors as mysql_errors

def get_connection():
    return mysql.connector.connect(
        host = "localhost",
        user = "root",
        password = "@Zingisa24",
        database = "syncpoint_archive"
    )

def hash_password(plain_password: str) -> str:
    salt = secrets.token_hex(16)
    hashed = hashlib.sha256((salt + plain_password).encode()).hexdigest()
    return f"{salt}${hashed}"

def verify_password(plain_password: str, stored_hash: str) -> bool:
    try:
        salt, hashed = stored_hash.split("$")
    except ValueError:
        return False
    attempt_hash = hashlib.sha256((salt + plain_password).encode()).hexdigest()
    return attempt_hash == hashed

def register_patient(
        username, plain_password, title, first_name, middle_name, last_name,
        preferred_name, id_number, date_of_birth,
        country, address_line, city, postal_code, province,
        home_phone, work_phone, mobile_phone, secondary_phone,
        email, secondary_email,
        has_medical_aid, provider, membership_number
):
    conn = get_connection()
    cursor = conn.cursor()

    password_hash = hash_password(plain_password)

    args = (
        username, password_hash, title, first_name, middle_name, last_name,
        preferred_name, id_number, date_of_birth,
        country, address_line, city, postal_code, province,
        home_phone, work_phone, mobile_phone, secondary_phone,
        email, secondary_email,
        1 if has_medical_aid else 0, provider, membership_number,
        0
    )
    try:
        result_args = cursor.callproc("sp_register_patient", args)
        conn.commit()
        new_patient_id = result_args[-1]

        return True, new_patient_id

    except mysql_errors.IntegrityError as e:
        conn.rollback()
        if "username" in str(e).lower():
            return False, "That username is already taken."
        elif "id_number" in str(e).lower():
            return False, "An Account with that Id Number already exists."
        else:
            return False, "Registration failed - some details are already in use."

    except mysql_errors.Error as e:
        conn.rollback()
        return False, f"Database error: {e}"

    finally:
        cursor.close()
        conn.close()

def get_patient_login_data(username):
    conn = get_connection()
    cursor = conn.cursor(dictionary= True)
    cursor.execute(""" 
    SELECT u.user_id, u.password_hash, p.patient_id, p.status, p.preferred_name
    FROM users u
    JOIN patients p ON p.user_id = u.user_id
    WHERE u.username = %s AND u.role = 'PATIENT'
    """, (username,))
    result = cursor.fetchone()
    cursor.close()
    conn.close()
    return result

def get_staff_login_data(username):
    conn = get_connection()
    cursor = conn.cursor(dictionary= True)
    cursor.execute(""" 
    SELECT u.user_id, u.password_hash, s.staff_id, s.first_name, s.last_name
    FROM users u
    JOIN staff s ON s.user_id = u.user_id
    WHERE u.username = %s AND u.role = 'STAFF'
    """, (username,))
    result = cursor.fetchone()
    cursor.close()
    conn.close()
    return result

#Creating a staff account
def create_staff_account_web(first_name, last_name, job_title_typed, courtesy_title, email, specialization, department):
    """
    Web-form version of staff_setup.py's create_staff_account(), used from the
    Admin Dashboard's 'Add Staff Member' panel. Returns
    (success, message, credentials_dict_or_None).
    """
    from staff_setup import get_role_digit, generate_staff_number, generate_username, generate_strong_password

    role_digit = get_role_digit(job_title_typed)
    if role_digit is None:
        return False, "Unknown Job title.", None

    job_title = job_title_typed.title()
    conn = get_connection()
    cursor = conn.cursor()

    try:
        #generating a staff number
        staff_number = generate_staff_number(cursor, role_digit)

        #generating a username
        username = generate_username(first_name, staff_number, job_title)

        #saving the original username
        base_username = username
        attempt = 1

        #check if the username exists or not
        cursor.execute("SELECT 1 FROM users WHERE username = %s", (username,))
        while cursor.fetchone() is not None:
            attempt += 1
            username = f"{base_username}{attempt}"
            cursor.execute("SELECT 1 FROM users WHERE username = %s", (username,))

        plain_password = generate_strong_password()
        password_hash = hash_password(plain_password)

#Registering/ putting staff Members in the database and get back with new staff ID
        args = (
            username, password_hash, first_name, last_name,
            staff_number, job_title, department,
            0
        )
        result_args = cursor.callproc("sp_register_staff", args)
        conn.commit()
        new_staff_id = result_args[-1]


def get_pending_patients():
    conn = get_connection()
    cursor = conn.cursor(dictionary= True)
    cursor.execute(""" 
    SELECT patient_id, first_name, last_name, id_number, created_at
    FROM patients
    WHERE status = 'PENDING'
    ORDER BY created_at ASC
    """)
    results = cursor.fetchall()
    cursor.close()
    conn.close()
    return results

def update_patient_status(patient_id, new_status, reviewed_by_staff_id):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute(""" 
    UPDATE patients
    SET status = %s,
        reviewed_by_staff_id = %s,
        reviewed_at = NOW()
    WHERE patient_id = %s
    """,
    (new_status, reviewed_by_staff_id, patient_id)
    )
    conn.commit()
    cursor.close()
    conn.close()

def format_staff_display_name(job_title, courtesy_title, last_name):
    if job_title == "Doctor":
        return f"Dr. {last_name}"
    return f"{job_title} {courtesy_title or ''} {last_name}".replace("  ", " ").strip()

def get_recent_activity_for_patient(patient_id):
    conn = get_connection()
    cursor = conn.cursor(dictionary= True)
    cursor.execute(""" 
    SELECT p.status, p.reviewed_at,
        s.job_title, s.courtesy_title, s.last_name
    FROM patients p
    LEFT JOIN staff s ON s.staff_id = p.reviewed_by_staff_id
    WHERE p.patient_id = %s
    """,
    (patient_id,)
    )
    row = cursor.fetchone()
    cursor.close()
    conn.close()

    if not row or not row["reviewed_at"]:
        return []

    action = "Registration approved" if row["status"] == "APPROVED" else "Registration rejected"
    display_name = format_staff_display_name(
        row["job_title"], row["courtesy_title"], row["last_name"]
    )

    return [{
        "Action": action,
        "By": display_name,
        "Date": row["reviewed_at"].strftime("%Y-%m-%d")
    }]