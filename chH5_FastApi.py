from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import sqlite3

# FastAPI 인스턴스 생성
app = FastAPI()

# SQLite 데이터베이스 연결 함수
def get_db_connection():
    conn = sqlite3.connect('C:/Users/admin/Documents/GitHub/medical-treatment/ChungcheongHospital.db')
    conn.row_factory = sqlite3.Row
    return conn

# 병원 정보를 가져오는 함수 정의
def get_hospitals():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM hospitals")
    hospitals = cursor.fetchall()
    conn.close()
    return hospitals

# FastAPI 모델 정의 (Pydantic을 사용하여 데이터 검증)
class Hospital(BaseModel):
    Hospital_name: str
    Hospital_Map: str
    medical_subject: str
    phone_number: str  # 전화번호는 일반적으로 문자열로 처리됩니다
    latitude: float | None
    longitude: float | None

# API 엔드포인트 정의
@app.get("/hospitals/", response_model=list[Hospital])
def read_hospitals():
    try:
        hospitals = get_hospitals()
        return [Hospital(
            Hospital_name=hospital["Hospital_name"],
            Hospital_Map=hospital["Hospital_Map"],
            medical_subject=hospital["medical_subject"],
            phone_number=str(hospital["phone_number"]), 
            latitude=hospital["latitude"] if hospital["latitude"] is not None else 0.0,
            longitude=hospital["longitude"] if hospital["longitude"] is not None else 0.0
        ) for hospital in hospitals]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# FastAPI 실행 (개발 서버로 실행)
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.1.13", port=8000)