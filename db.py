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

