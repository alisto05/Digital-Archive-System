from datetime import datetime
from fpdf import FPDF

def _label(key: str) -> str:
    return key.replace("_", " ").title()

def _text(value) -> str:
    if value is None or value == "":
        return "-"
    if isinstance(value, bool):
        return "Yes" if value else "No"
    return str(value).encode("latin-1", "replace").decode("latin-1")

def make_pdf(title: str, rows: list[dict], columns: list[str] | None = None) -> bytes:
    pdf = FPDF(orientation="L", unit="mm", format= "A4")
    pdf.set_auto_page_break(auto= True, margin= 15)
    pdf.add_page()
    pdf.set_font("Helvetica", "B", 16)
    pdf.cell(0, 10, _text(title), new_x= "LMARGIN", new_y= "NEXT")
    pdf.set_font("Helvetica", "", 9)
    pdf.cell(0, 6, f"SyncPoint Digital Archive - Generated {datetime.now():%Y-%m-%d %H:%M}",
             new_x= "LMARGIN", new_y= "NEXT")
    pdf.ln(3)

    if not rows:
        pdf.set_font("Helvetica", "", 11)
        pdf.cell(0, 8, "No records.", new_x= "LMARGIN", new_y= "NEXT")
        return bytes(pdf.output())

    cols = columns or list(rows[0].keys())

    pdf.set_font("Helvetica", "", 8)
    with pdf.table(text_align= "LEFT", first_row_as_headings= True) as table:
        header = table.row()
        for c in cols:
            header.cell(_label(c))
        for r in rows:
            row = table.row()
            for c in cols:
                row.cell(_text(r.get(c)))

    return bytes(pdf.output())
