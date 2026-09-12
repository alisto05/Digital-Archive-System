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