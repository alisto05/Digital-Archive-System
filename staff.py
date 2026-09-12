import string
import secrets
from db import get_connection, hash_password
from mysql.connector import errors as mysql_errors

JOB_ROLE_DIGITS = {
    "doctor": "1",
    "nurse": "4"
    "receptionist": "5"
}

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