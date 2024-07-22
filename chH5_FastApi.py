from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import json

# FastAPI 인스턴스 생성
app = FastAPI()

# FastAPI 모델 정의 (Pydantic을 사용하여 데이터 검증)
class Hospital(BaseModel):
    name: str
    address: str
    subject: str
    lat: float | None
    lng: float | None

# JSON 파일에서 병원 정보를 가져오는 함수 정의
def get_hospitals_from_json(file_path):
    with open(file_path, 'r', encoding='utf-8') as json_file:
        data = json.load(json_file)
        hospitals = data.get('data', [])
        return hospitals

# JSON 파일 경로 설정
json_file_path = 'Hospital.json'

# API 엔드포인트 정의
@app.get("/hospitals/", response_model=list[Hospital])
def read_hospitals():
    try:
        hospitals = get_hospitals_from_json(json_file_path)
        return [Hospital(
            name=hosp["name"],
            address=hosp["address"],
            subject=hosp["subject"],
            lat=float(hosp["lat"]) if hosp["lat"] is not None else None,
            lng=float(hosp["lng"]) if hosp["lng"] is not None else None
        ) for hosp in hospitals]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# FastAPI 실행 (개발 서버로 실행)
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.171.6", port=8000)
