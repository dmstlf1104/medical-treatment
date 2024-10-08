from fastapi import FastAPI, File, UploadFile, Body
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from anthropic import Anthropic
from langchain_core.prompts import ChatPromptTemplate
import mysql.connector
import requests
import re
import time
from io import BytesIO
from PIL import Image
from typing import Dict

# FastAPI 앱 초기화
app = FastAPI()

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 사용자 ID 저장 (메모리 내)
user_id_storage: Dict[str, str] = {}

MODEL_NAME = "claude-3-sonnet-20240229"

# Claude API 클라이언트 초기화
anthropic = Anthropic(api_key="o")

# 프롬프트 템플릿 정의
prompt = ChatPromptTemplate.from_messages([
    ("system", "당신은 AI 의료 어시스턴트 힐리입니다. 다음 의료 보고서를 보고 아무것도 모르는 환자들이 이해하기 쉽고 귀엽게 대답해주세요. 예를 들어 결과는 ---- 나왔습니다. 자세한 상담은 의사 선생님과 하시길 바라요!. 의료 보고서:"),
    ("user", "{input}")
])

# OCR 서비스 API 키
api_key = "o"
url = "o"
headers = {"Authorization": f"Bearer {api_key}"}

def preprocess_text(text):
    text = re.sub(r'\s+', ' ', text).strip()
    text = re.sub(r'[^\w\s.,?]', '', text)
    text = re.sub(r'\n+', '\n', text)
    text = '. '.join(s.capitalize() for s in text.split('. '))
    return text

def retrieval_qa_chain(input_text, documents):
    full_prompt = prompt.format(input=documents + "\n" + input_text)
    response = anthropic.messages.create(
        model="claude-3-sonnet-20240229", 
        max_tokens=1000,
        messages=[
            {"role": "user", "content": full_prompt}
        ]
    )
    return response.content[0].text


# 데이터베이스 연결 정보
db_config = {
    'host': '192.168.247.41',
    'user': 'tester',
    'password': '1234',
    'database': 'medical_records_db',
}

@app.post("/sendUserId")
async def send_user_id(userId: str = Body(...)):
    try:
        print(f"Received user ID: {userId}")

        # "userId=" 부분을 제거하고 숫자만 추출
        user_id_value = re.sub(r'^userId=', '', userId).strip()

        # 사용자 ID 저장
        user_id_storage['current_user_id'] = user_id_value

        return JSONResponse(content={"message": "사용자 ID가 성공적으로 수신되었습니다."})
    except Exception as e:
        error_msg = str(e)
        print(f"Error during user ID processing: {error_msg}")
        return JSONResponse(content={"error": error_msg}, status_code=500)

@app.post("/upload")
async def upload_file(file: UploadFile = File(...)):
    try:
        # 사용자 ID 가져오기
        user_id = user_id_storage.get('current_user_id')
        if not user_id:
            return JSONResponse(content={"error": "사용자 ID가 저장되어 있지 않습니다."}, status_code=400)

        # 이미지 파일 읽기
        image_bytes = await file.read()
        try:
            image = Image.open(BytesIO(image_bytes))
        except Exception as e:
            return JSONResponse(content={"error": f"이미지 처리 오류: {str(e)}"}, status_code=500)

        with BytesIO() as buffer:
            image.save(buffer, format="PNG")
            image_data = buffer.getvalue()  # 바이너리 데이터

        # OCR API 요청
        with BytesIO(image_bytes) as buffer:
            files = {"document": ("image.png", buffer, "image/png")}
            response = requests.post(url, headers=headers, files=files)
            if response.status_code == 200:
                result = response.json()
                text = result.get('text', '')
                documents = preprocess_text(text)
                
                user_input = "병원에서 받은 검사 결과지를 해석해줘"
                
                full_prompt = prompt.format(input=documents + "\n" + user_input)
                claude_response = anthropic.messages.create(
                    model="claude-3-sonnet-20240229",  # 문자열로 모델명 지정
                    max_tokens=1000,
                    messages=[
                        {"role": "user", "content": full_prompt}
                    ]
                )
                analysis_result = claude_response.content[0].text

                # 결과 구성
                result_array = [
                    {"term_ko": "OCR 추출 텍스트", "term_en": "", "explanation": documents},
                    {"term_ko": "분석 결과", "term_en": "", "explanation": analysis_result}
                ]

                # 데이터베이스에 OCR 결과 및 분석 결과, 이미지 데이터 삽입
                connection = mysql.connector.connect(**db_config)
                cursor = connection.cursor()
                try:
                    insert_data_query = """
                        INSERT INTO medical_records (user_id, ocr_text, analysis_result, ocr_image)
                        VALUES (%s, %s, %s, %s)
                    """
                    cursor.execute(insert_data_query, (user_id, documents, analysis_result, image_data))
                    connection.commit()
                    print("데이터가 성공적으로 삽입되었습니다.")
                except mysql.connector.Error as db_error:
                    print(f"Database insertion error: {db_error}")
                    return JSONResponse(content={"error": "데이터베이스 삽입 오류"}, status_code=500)
                finally:
                    cursor.close()
                    connection.close()

                return JSONResponse(content=result_array)
            else:
                error_msg = f"OCR API 요청 실패: {response.status_code} - {response.text}"
                return JSONResponse(content=[{"term_ko": "Error", "term_en": "", "explanation": error_msg}], status_code=response.status_code)
    except Exception as e:
        error_msg = f"오류 발생: {str(e)}"
        print(error_msg)
        return JSONResponse(content=[{"term_ko": "Error", "term_en": "", "explanation": error_msg}], status_code=500)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.247.188", port=8000)