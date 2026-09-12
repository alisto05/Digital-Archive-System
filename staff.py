import string
import secrets
from db import get_connection, hash_password
from mysql.connector import errors as mysql_errors

JOB_ROLE_DIGITS = {
    #1-3 set for Doctor type role
    "doctor": "1",
    #4-9 set for nurses
    "nurse": "4",
    #5 are set for receptionist
    "receptionist": "5",
    #6-9 stored for future job title (Admin, Lab Tech, Pharmacit)
}
def get_role_digit(job_title):
    return JOB_ROLE_DIGITS.get(job_title.strip().lower())

def generate_staff_number(cursor, role_digit):
    pattern = f"S-{role_digit}_______" #S- + role digit + exactly 7 numbers
    cursor.execute("SELECT staff_number FROM staff WHERE staff_number LIKE %s", (pattern,))
    existing_numbers = cursor.fetchall()

    highest_seq = 0
    for (staff_number,) in existing_numbers:
        seq_part = staff_number[3:] # striping "S-" and the role digit
        seq = int(seq_part)
        if seq > highest_seq:
            highest_seq = seq

    next_seq = highest_seq + 1
    padded = str(next_seq).zfill(7)

    return f"S-{role_digit}{padded}"

def generate_username(first_name, staff_number, job_title):
    name_part = first_name[:6]
    digit_only = "".join(ch for ch in staff_number if ch.isdigit())
    number_part = digits_only[-2:]
    title_part = job_title[0].upper()

    return f"S-{name_part}{number_part}&{title_part}"

def generate_staff_number(cursor):
    """
    Wantt to build a staff number that takes/ starts with 'S-'
    and begin with a number 4 then random numbers but less than 8
    """
    cursor.execute(""" 
    SELECT AUTO_INCREMENT
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff'
    """)
    (next_id,) = cursor.fetchone()

    #7 numbers