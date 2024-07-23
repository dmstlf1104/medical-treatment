from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import json

# FastAPI 인스턴스 생성
app = FastAPI()

# FastAPI 모델 정의 (Pydantic을 사용하여 데이터 검증)
class Hospital(BaseModel):
    name: str
    address: str
    subject: str
    phone: Optional[str] = None  # Optional로 변경하여 값이 없을 때 None을 허용
    lat: Optional[float] = None  # Optional로 변경하여 값이 없을 때 None을 허용
    lng: Optional[float] = None  # Optional로 변경하여 값이 없을 때 None을 허용
    time: Optional[str] = None  # Optional로 변경하여 값이 없을 때 None을 허용

# JSON 파일에서 병원 정보를 가져오는 함수 정의
def get_hospitals_from_json(file_path: str):
    with open(file_path, 'r', encoding='utf-8') as json_file:
        data = json.load(json_file)
        hospitals = data.get('data', [])
        return hospitals

# JSON 파일 경로 설정
json_file_path = 'merged_hospitals.json'

# API 엔드포인트 정의
@app.get("/hospitals/", response_model=List[Hospital])
def read_hospitals():
    try:
        hospitals = get_hospitals_from_json(json_file_path)
        return [Hospital(
            name=hosp.get("name"),
            address=hosp.get("address"),
            subject=hosp.get("subject"),
            phone=hosp.get("phone"),  # 필드가 없으면 None으로 처리됨
            lat=hosp.get("lat"),  # 필드가 없으면 None으로 처리됨
            lng=hosp.get("lng"),  # 필드가 없으면 None으로 처리됨
            time=hosp.get("time")  # 필드가 없으면 None으로 처리됨
        ) for hosp in hospitals]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# FastAPI 실행 (개발 서버로 실행)
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.171.5", port=8000)
