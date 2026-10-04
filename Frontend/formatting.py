#Every date on goes through here
import re
from datetime import datetime, timedelta, timezone

#Backend sends UTC times (the long ones) and SA is UTC+2 
LOCAL_UTC_OFFSET_HOURS = 2

DATE_TIME_FORMAT = "%Y-%m-%d %H:%M"
DATE_FORMAT = "%Y-%m-%d"

_ALREADY_FORMATTED = re.compile(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}")

def _parse(value):
    if value is None:
        return
    text = str(value).strip()
    if len(text) < 11:
        return None
    try:
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
    except ValueError:
        return None
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo= timezone.utc)
    return parsed.astimezone(timezone(timedelta(hours= LOCAL_UTC_OFFSET_HOURS)))

#------Making ("2026-10-03T11:14:09.000+00:00 -> 2026-10-03 02:06")-----
def fmt_datetime(value) -> str:
    if value in (None, ""):
        return "-"
    if _ALREADY_FORMATTED.fullmatch(str(value).strip()):
        return str(value).strip()
    parsed = _parse(value)
    return parsed.strftime(DATE_TIME_FORMAT) if parsed else str(value)

def fmt_date(value) -> str:
    if value in (None, ""):
        return "-"
    parsed = _parse(value)
    return parsed.strftime(DATE_FORMAT) if parsed else str(value)[:10]

def _is_date_column(key: str) -> bool:
    k = str(key).lower()
    return k.endswith("_at") or k.endswith(" at") or k in ("date", "registered", "created")

#Returns a copy ofthe rows where every date colummn is fromatted----
def format_rows(rows):
    cleaned = []
    for row in rows or []:
        new_row = dict(row)
        for key, value in row.items():
            if _is_date_column(key):
                new_row[key] = fmt_datetime(value)
        cleaned.append(new_row)
    return cleaned
