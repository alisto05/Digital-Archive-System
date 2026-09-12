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

def username_exists(cursor, username):
    cursor.execute("SELECT 1 FROM users WHERE username = %s", (username,))
    return cursor.fetchone() is not None

def generate_strong_password(length= 12):
    if length < 10:
        length = 10
    lowercase = string.ascii_lowercase
    uppercase = string.ascii_uppercase
    digits = string.digits
    symbols = "!@#$%^&*"

    password_chars = [
        secrets.choice(lowercase),
        secrets.choice(uppercase),
        secrets.choice(digits),
        secrets.choice(symbols),
    ]

    all_chars = lowercase + uppercase + digits + symbols
    remaining_length = length - len(password_chars)
    password_chars += [secrets(all_chars) for _ in range(remaining_length)]

    secrets.SystemRandom().shuffle(password_chars)

    return "".join(password_chars)

def create_staff_account():
    print("=== SyncPoint Staff Account Setup ===\n")

    first_name = input("First Name: ").strip()
    last_name = input("Last Name: ").strip()

    role_digit = None
    while role_digit is None:
        typed_title = input("Job Title (Doctor/ Nurse/ Receptionist): ").strip()
        role_digit = get_role_digit(typed_title)
        if role_digit is None:
            known - ", ".join(t.title() for t in JOB_ROLE_DIGITS)
            print(f"Unknown job title. Known titles: {known_titles}")

    job_title = typed_title.title()

    courtesy_title = None
    if job_title != "Doctor":
        while courtesy_title not in ("Mr.", "Mrs.", "Miss."):
            courtesy_title = input("Courtesy Title (Mr. / Mrs. / Miss.): ").strip()
            if courtesy_title not in ("Mr.", "Mrs.", "Miss."):
                print("Please enter exactly one of: Mr., Mrs., Miss.")

    department = input("Department: ").strip()

    conn = get_connection()
    cursor = conn.cursor()

    staff_number = generate_staff_number(cursor, role_digit)
    username = generate_username(first_name, staff_number, job_title)
    base_username = username
    attempt = 1
    while username_exists(cursor, username):
        attempt += 1
        username = f"{base_username}{attempt}"

    plain_password = generate_strong_password()

    print("\nGenrate account details:")
    print(f" Username:      {username}")
    print(f" Staff Number:  {staff_number}")
    print(f" Password:      {plain_password}")
    if courtesy_title:
        print(f"Will display to patients as: Dr. {job_title} {courtesy_title} {last_name}")
    else:
        print(f" Will display to patients as Dr. {last_name}")
    confirm = input("\nCreate this account? (y/n): ").strip().lower()

    if confirm != "y":
        print("Cancelled, No Account created.")
        cursor.close()
        conn.close()
        return

    password_hash = hash_password(plain_password)

    args = (
        username, password_hash, first_name, last_name,
        staff_number, job_title, department,
        0
    )

    try:
        result_args = cursor.callproc("sp_register_staff", args)
        conn.commit()
        new_staff_id = result_args[-1]

        if courtesy_title:
            cursor.execute(
                "UPDATE staff SET courtesy_title = %s WHERE staff_id = %s",
                (courtesy_title, new_staff_id)
            )
            conn.commit()

        print(f"\nStaff account created. staff_id = {new_staff_id}")
        print("Give the username and password above to the new staff member securely")
        print("(not over an unsercured chat/ email - this password is shown only once here).")

    except mysql_errors.IntegrityError:
        conn.rollback()
        print("\nFailed: That username or Staff Number is already in use.")

    except mysql_errors.Error as e:
        conn.rollback()
        print(f"\nDatabase error: {e}")

    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    create_staff_account()

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