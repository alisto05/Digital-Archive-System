FROM python:3.12-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
STREAMLIT_BROWSER_GATHER_USAGE_STATS=false \
APP_ENTRY=home.py

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .
EXPOSE 8501
CMD ["sh", "-c", "streamlit run ${APP_ENTRY} --server.port=8501 --server.address=0.0.0.0"]
