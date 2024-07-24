import os
import requests
import re
import time
from io import BytesIO
from PIL import Image
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import HTMLResponse, JSONResponse
from langchain.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate

# Initialize the FastAPI app
app = FastAPI()

# Initialize the language model
llm = Ollama(model="bnksys/yanolja-eeve-korean-instruct-10.8b:latest")

# Define the prompt template
prompt = ChatPromptTemplate.from_messages([
    ("system", "당신은 AI 의료 어시스턴트 힐리입니다. 다음 의료 보고서를 보고 아무것도 모르는 환자들이 이해하기 쉽고 귀엽게 대답해주세요. 예를 들어 결과는 ---- 나왔습니다. 자세한 상담은 의사 선생님과 하시길 바라요!. 의료 보고서:"),
    ("user", "{input}")
])

# API key for OCR service
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
    response = llm(full_prompt)
    end_time = time.time()
    print(f"Prediction time: {end_time - start_time:.2f} seconds")
    return response

@app.get("/", response_class=HTMLResponse)
async def read_root():
    return """
    <html>
        <head>
            <title>의료 보고서 해석기</title>
        </head>
        <body>
            <h1>의료 보고서 파일을 업로드하세요</h1>
            <form action="/upload" enctype="multipart/form-data" method="post">
                <input name="file" type="file" accept=".pdf,.jpg,.jpeg,.png">
                <input type="submit">
            </form>
        </body>
    </html>
    """

@app.post("/upload/")
async def upload_file(file: UploadFile = File(...)):
    try:
        # Read and process the image file
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

            # Example user input; in a real app, this should come from the request
            user_input = "병원에서 받은 검사 결과지를 해석해줘"
            result = retrieval_qa_chain(user_input, documents)

            return JSONResponse(content={"ocr_text": documents, "analysis_result": result})
        else:
            return JSONResponse(content={"error": "OCR 추출에 실패했습니다."}, status_code=response.status_code)
    except Exception as e:
        return JSONResponse(content={"error": str(e)}, status_code=500)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.171.5", port=8000)
