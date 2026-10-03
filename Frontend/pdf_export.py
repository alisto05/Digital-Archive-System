import io
import os
import secrets
from datetime import datetime
from fpdf import FPDF
from fpdf.enums import EncryptionMethod
from fpdf.encryption import AccessPermission
from PIL import Image
from formatting import fmt_date, format_rows

LOGO_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "assets", "logo-removebg.png")
BRAND = (21, 71, 160)   #The blue on in the logo
WATERMARK_OPACITY = 0.07    #0= invisible, 1 = solid
WATERMARK_SIZE_MM = 22  #size of every small logo
WATERMARK_ANGLE = 30    #degrees of small logo sliding


def _text(value) -> str:
    if value is None or value == "":
        return "-"
    if isinstance(value, bool):
        return "Yes" if value else "No"
    return str(value).encode("latin-1", "replace").decode("latin-1")

def _label(key: str) -> str:
    return key.replace("_", " ").title()

#tilt copy of the logo, made once and reused for every position on every page-------
def _watermark_tile() -> io.BytesIO | None:
    if not os.path.exists(LOGO_PATH):
        return None
    logo = Image.open(LOGO_PATH).convert("RGBA")
    red, green, blue, alpha = logo.split()
    alpha = alpha.point(lambda a: int(a * WATERMARK_OPACITY))
    logo = Image.merge("RGBA", (red, green, blue, alpha))
    logo = logo.rotate(WATERMARK_ANGLE, expand= True, resample= Image.BICUBIC)
    buffer = io.BytesIO()
    logo.save(buffer, format= "PNG")
    buffer.seek(0)
    return buffer

#Every page auto gets the small logos all in it------

class BrandedPDF(FPDF):
    def __init__(self, *args, show_header_logo= True, **kwargs):
        super().__init__(*args, **kwargs)
        self._title = _watermark_tile()
        self._show_header_logo = show_header_logo

    def header(self):
        if self._title is not None:
            size = WATERMARK_SIZE_MM
            step_x, step_y = size * 2.2, size * 2.0
            row = 0
            y = 4
            while y < self.h:
    #---------Every 2nd row is shifted so it looks like its scattered---------
                x = 4 + (step_x / 2 if row % 2 else 0)
                while x < self.w:
                    self._title.seek(0)
                    self.image(self._title, x= x, y= y, w= size)
                    x += step_x
                y += step_y
                row += 1
        self.set_xy(self.l_margin, self.t_margin)

    def footer(self):
        self.set_y(-12)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(110, 110, 110)
        self.cell(0, 6, f"SyncPoint Hospital Digital Archive System   |  Page {self.page_no()}", align= "C")
        self.set_text_color(0, 0, 0)

def _top_logo(pdf: FPDF, x= None, y= 8, w= 28):
    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x= pdf.l_margin if x is None else x, y= y, w= w)


#-------------Table Report-----------
def make_pdf(title: str, rows: list[dict], columns: list[str] | None = None) -> bytes:
    rows = format_rows(rows)
    pdf = FPDF(orientation="L", unit="mm", format= "A4")
    pdf.set_auto_page_break(auto= True, margin= 15)
    pdf.add_page()

    _top_logo(pdf, w= 26)
    pdf.set_xy(pdf.l_margin + 30, 10)
    pdf.set_font("Helvetica", "B", 16)
    pdf.set_text_color(*BRAND)
    pdf.cell(0, 8, _text(title), new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_x(pdf.l_margin + 30)
    pdf.set_font("Helvetica", "", 9)
    pdf.set_text_color(90, 90, 90)
    pdf.cell(0, 6, f"SyncPoint Digital Archive - issued {datetime.now():%Y-%m-%d %H:%M}",
             new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_text_color(0, 0, 0)
    pdf.set_y(max(pdf.get_y(), 10 + 26) + 2)

    if not rows:
        pdf.set_font("Helvetica", "", 11)
        pdf.cell(0, 8, "No records.", new_x= "LMARGIN", new_y= "NEXT")
        return bytes(pdf.output())

    cols = columns or list(rows[0].keys())

    pdf.set_font("Helvetica", "", 8)
    with pdf.table(text_align= "LEFT", first_row_as_headings= True) as table:
        header = table.row()
        for column in cols:
            header.cell(_label(column))
        for record in rows:
            row = table.row()
            for column in cols:
                row.cell(_text(record.get(column)))

    return bytes(pdf.output())

#Stamp in the right side corner (similar to POR)----
def _stamp(pdf: FPDF, lines: list[tuple[str, str]], x: float, y: float, w= 62):
    line_height = 5.2
    h = 31 + line_height * len(lines)
    pdf.set_draw_color(*BRAND)
    pdf.set_line_width(0.7)
    pdf.rect(x, y, w, h, style= "D", round_corners= True, corner_radius= 3)
    pdf.set_line_width(0.2)
    if os.path.exists(LOGO_PATH):
        pdf.image(LOGO_PATH, x= x + (w - 24)/ 2, y= y + 1.5, w= 24)
    pdf.set_xy(x, y + 22)
    pdf.set_font("Helvetica", "B", 8)
    pdf.set_text_color(*BRAND)
    pdf.cell(w, 4, "SYNCPOINT", align= "C", new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_font("Helvetica", "", 8)
    pdf.set_text_color(40, 40, 40)
    cursor = y + 22 + 5
    for label, value in lines:
        pdf.set_xy(x + 3, cursor)
        pdf.set_font("Helvetica", "B", 8)
        pdf.cell(w * 0.50, line_height, _text(label))
        pdf.set_font("Helvetica", "", 8)
        pdf.cell(w * 0.50 - 6, line_height, _text(value), align= "R")
        cursor += line_height
    pdf.set_text_color(0, 0, 0)
    pdf.set_draw_color(0, 0, 0)

#Making Printing be the only one that is allowed------
def _locked_output(pdf: FPDF) -> bytes:
    pdf.set_encryption(
        owner_password= secrets.token_urlsafe(24),
        permissions= AccessPermission.PRINT_LOW_RES | AccessPermission.PRINT_HIGH_RES,
        encryption_method= EncryptionMethod.AES_128,
    )
    return bytes(pdf.output())


def _certificate(heading, intro, name, sub_lines, statement, detail_lines, reference, stamp_lines, footer_note):
    pdf = BrandedPDF(orientation= "P", unit= "mm", format= "A4")
    pdf.set_auto_page_break(auto= False)
    pdf.set_margins(18, 14, 18)
    pdf.add_page()

    _top_logo(pdf, x= 18, y= 10, w= 40)

    pdf.set_xy(18, 56)
    pdf.set_font("Helvetica", "B", 17)
    pdf.set_text_color(*BRAND)
    pdf.cell(0, 9, _text(heading), align= "C", new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_text_color(0, 0, 0)

    pdf.ln(4)
    pdf.set_font("Helvetica", "", 10)
    pdf.cell(0, 6, _text(intro), align= "C", new_x= "LMARGIN", new_y= "NEXT")

    pdf.ln(4)
    pdf.set_font("Helvetica", "B", 15)
    pdf.cell(0, 8, _text(name), align= "C", new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_font("Helvetica", "", 10)
    for line in sub_lines:
        pdf.cell(0, 6, _text(line), align= "C", new_x= "LMARGIN", new_y= "NEXT")

    pdf.ln(6)
    pdf.set_font("Helvetica", "", 10)
    pdf.multi_cell(0, 6, _text(statement), align= "C", new_x= "LMARGIN", new_y= "NEXT")

    pdf.ln(6)
    for label, value in detail_lines:
        pdf.set_x(40)
        pdf.set_font("Helvetica", "B", 10)
        pdf.cell(55, 77, _text(label))
        pdf.set_font("Helvetica", "", 10)
        pdf.cell(0, 7, _text(value), new_x= "LMARGIN", new_y= "NEXT")

    pdf.set_xy(18, 218)
    pdf.set_font("Helvetica", "B", 10)
    pdf.cell(0, 6, f"Reference Number: {_text(reference)}", new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_font("Helvetica", "I", 8)
    pdf.set_text_color(90, 90, 90)
    pdf.multi_cell(105, 4.5, _text(footer_note), new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_text_color(0, 0, 0)

    _stamp(pdf, stamp_lines, x= pdf.w - 18 - 62, y= 208)
    return _locked_output(pdf)

#Proof that the patient is registered at SyncPoint--------
def make_patient_confirmation(patient_id, profile: dict, approve_at, approved_by) -> bytes:
    full_name = " ".join(
        part for part in (profile.get("title"), profile.get("first_name"),
                          profile.get("middle_name"), profile.get("last_name")
                          ) if part
    )
    issued = datetime.now()
    registered = fmt_date(approve_at)
    return _certificate(
        heading = f"PROOF OF REGISTRATION {issued.year}",
        intro = "It is hereby certified that",
        name= full_name,
        sub_lines= [f"(Date of Birth: {fmt_date(profile.get('date_of_birth'))})",
                    f"(Id Number: {profile.get('id_number') or "-"})"],
                    statement= "is registered as a patient of the SyncPoint Hospital Digital Archive System.",
                    detail_lines= [
                        ("Status", "REGISTERED (approved)"),
                        ("Registration approved by", approved_by or "-"),
                        ("Registration date", registered),
                    ],
                    reference= f"SP-P-{int(patient_id):06d}",
                    stamp_lines= [("Date of issue", f"{issued:%Y-%m-%d}"), ("Registered", registered)],
                    footer_note= "Issued electronically by SyncPoint. No signature is required. This document is for printing ONLY.",
    )

#Proof that the person is a registered staff member------
def make_staff_confirmation(staff: dict) -> bytes:
    title = staff.get("courtesy_title") or ""
    full_name = f"{title} {staff.get('first_name', '')} {staff.get('last_name', '')}".strip()
    issued = datetime.now()
    registered = fmt_date(staff.get("created_at"))
    details = [
        ("Role", staff.get("job_title")),
        ("Department", staff.get("department")),
        ("Staff Number", staff.get("staff_number")),
    ]
    if staff.get("specialization"):
        details.append(("Specialization", staff.get("specialization")))
    details.append(("Registration Date", registered))
    return _certificate(
        heading = f"CONFIRMATION OF STAFF REGISTRATION {issued.year}",
        intro= "It is hereby certified that",
        name= full_name,
        sub_lines= [f"(Staff Number: {staff.get('staff_number') or '-'})"],
        statement= "is registered as a member of staff of the SyncPoint Hospital Digital Archive System.",
        detail_lines= details,
        reference= f"SP-S-{int(staff.get('staff_id') or 0):06d}",
        stamp_lines= [("Date of issue", f"{issued:%Y-%m-%d}"), ("Registered", registered)],
        footer_note= "Issued electronically by SyncPoint. No signature is required. This document is for printing ONLY.",
        )

