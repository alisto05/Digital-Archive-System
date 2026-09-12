#Using my own Local Host first

import hashlib
import secrets
import mysql.connector
from mysql.connector import errors as mysql_errors

def get_connection():
    return mysql.connector.connect(
        host = "localhost",
        user = "root",
        #password = "mypassword",
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
    SELECT u.user_id, u.password_hash, p'patient_id, p.status, p.preferred_name
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
    From users u
    JOIN staff s ON s.user_id = u.user_id
    WHERE U.username = %s AND u.role = 'STAFF'
    """, (username,))
    result = cursor.fetchone()
    cursor.close()
    conn.close()
    return result