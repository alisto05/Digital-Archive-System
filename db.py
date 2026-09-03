#Using my own Local Host first

import hashlib
import secrets
import mysql.connector
from mysql.connector import errors as mysql_errors

def get_connection():
    return mysql.connector.connect(
        host = "localhost",
        user = "root",
        password = "",
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