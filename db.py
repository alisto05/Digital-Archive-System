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

