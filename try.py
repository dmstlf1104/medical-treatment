import os
import requests
import re
import time
from io import BytesIO
from PIL import Image
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from langchain.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
import mysql.connector

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

# 언어 모델 초기화
llm = Ollama(model="bnksys/yanolja-eeve-korean-instruct-10.8b:latest")

# 프롬프트 템플릿 정의
prompt = ChatPromptTemplate.from_messages([
    ("system", "당신은 AI 의료 어시스턴트 힐리입니다. 다음 의료 보고서를 보고 아무것도 모르는 환자들이 이해하기 쉽고 귀엽게 대답해주세요. 예를 들어 결과는 ---- 나왔습니다. 자세한 상담은 의사 선생님과 하시길 바라요!. 의료 보고서:"),
    ("user", "{input}")
])

# OCR 서비스 API 키
api_key = "up_8sqFsKHvhK5EqZeiO8p8DfjXG2yZ7"
url = "https://api.upstage.ai/v1/document-ai/ocr"
headers = {"Authorization": f"Bearer {api_key}"}

def preprocess_text(text):
    text = re.sub(r'\s+', ' ', text).strip()
    text = re.sub(r'[^\w\s.,?]', '', text)
    text = re.sub(r'\n+', '\n', text)
    text = '. '.join(s.capitalize() for s in text.split('. '))
    return text

def retrieval_qa_chain(input_text, documents):
    full_prompt = prompt.format(input=documents + "\n" + input_text)
    start_time = time.time()
    response = llm.invoke(full_prompt)  # Deprecated method __call__ replaced with invoke
    end_time = time.time()
    return response

# 데이터베이스 연결 정보
db_config = {
    'host': '192.168.247.41',
    'user': 'tester',
    'password': '1234',
    'database': 'medical_records_db',
}

@app.post("/upload")
async def upload_file(file: UploadFile = File(...)):
    try:
        # 이미지 파일 읽기
        image = Image.open(BytesIO(await file.read()))
        with BytesIO() as buffer:
            image.save(buffer, format="PNG")
            buffer.seek(0)
            files = {"document": buffer}

            response = requests.post(url, headers=headers, files=files)
            if response.status_code == 200:
                result = response.json()
                text = result.get('text', '')
                documents = preprocess_text(text)

                # 예제 사용자 입력; 실제 앱에서는 요청에서 가져와야 합니다
                user_input = "병원에서 받은 검사 결과지를 해석해줘"
                analysis_result = retrieval_qa_chain(user_input, documents)

                # 결과 구성
                result_array = [
                    {"term_ko": "OCR 추출 텍스트", "term_en": "", "explanation": documents},
                    {"term_ko": "분석 결과", "term_en": "", "explanation": analysis_result}
                ]

                # 데이터베이스에 OCR 결과 및 분석 결과 삽입
                connection = mysql.connector.connect(**db_config)
                cursor = connection.cursor()
                try:
                    insert_data_query = "INSERT INTO medical_records (ocr_text, analysis_result) VALUES (%s, %s)"
                    cursor.execute(insert_data_query, (documents, analysis_result))
                    connection.commit()
                    print("데이터가 성공적으로 삽입되었습니다.")
                except mysql.connector.Error as db_error:
                    print(f"Database insertion error: {db_error}")
                finally:
                    cursor.close()
                    connection.close()

                return JSONResponse(content=result_array)
            else:
                error_msg = "OCR 추출에 실패했습니다."
                return JSONResponse(content=[{"term_ko": "Error", "term_en": "", "explanation": error_msg}], status_code=response.status_code)
    except Exception as e:
        error_msg = str(e)
        print(f"Error during file processing: {error_msg}")
        return JSONResponse(content=[{"term_ko": "Error", "term_en": "", "explanation": error_msg}], status_code=500)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.247.188", port=8000)
