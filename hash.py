import os
import hashlib
import re
import time
import asyncio
import aiohttp
from aiofiles import open as aio_open

from langchain.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate

llm = Ollama(model="bnksys/yanolja-eeve-korean-instruct-10.8b:latest")

prompt = ChatPromptTemplate.from_messages([
    ("system", "당신은 AI 의료 어시스턴트 힐리입니다. 다음 의료 보고서를 보고 아무것도 모르는 환자들이 이해하기 쉽고 귀엽게 대답해주세요. 예를 들어 결과는 ---- 나왔어요. 자세한 상담은 의사 선생님과 하시길 바라요!그리고  또한 의료 관련 질문이 아니라면 대답을 거절해도 됩니다. "),
    ("user", "{input}")
])

api_key = "up_8sqFsKHvhK5EqZeiO8p8DfjXG2yZ7"
filename = input("파일을 입력하세요 : ")

url = "https://api.upstage.ai/v1/document-ai/ocr"
headers = {"Authorization": f"Bearer {api_key}"}

def preprocess_text(text):
    text = re.sub(r'\s+', ' ', text).strip()
    text = re.sub(r'[^\w\s.,?]', '', text)
    text = re.sub(r'\n+', '\n', text)
    text = '. '.join(s.capitalize() for s in text.split('. '))
    return text

def get_cache_filename(filename):
    hash_object = hashlib.md5(filename.encode())
    return f"cache_{hash_object.hexdigest()}.txt"

async def get_ocr_text(filename):
    cache_filename = get_cache_filename(filename)
    if os.path.exists(cache_filename):
        async with aio_open(cache_filename, 'r') as cache_file:
            return await cache_file.read()
    
    async with aio_open(filename, "rb") as file:
        file_data = await file.read()
    
    async with aiohttp.ClientSession() as session:
        async with session.post(url, headers=headers, data=file_data) as response:
            if response.status == 200:
                result = await response.json()
                text = result.get('text', '')
                processed_text = preprocess_text(text)
                async with aio_open(cache_filename, 'w') as cache_file:
                    await cache_file.write(processed_text)
                return processed_text
            else:
                print(f"Error: {response.status}")
                print(await response.text())
                return "OCR 추출에 실패했습니다."

def retrieval_qa_chain(documents, input_text):
    full_prompt = prompt.format(input=documents + "\n" + input_text)
    start_time = time.time()
    response = llm(full_prompt)
    end_time = time.time()
    print(f"Prediction time: {end_time - start_time:.2f} seconds")
    return response

def process_result(result):
    # 문장을 배열로 변환 (여기서는 문단을 기준으로 분리)
    sentences = result.split('\n')
    sentences = [sentence.strip() for sentence in sentences if sentence.strip()]  # 공백 제거
    return sentences

async def main():
    documents = await get_ocr_text(filename)
    user_input = "병원에서 받은 검사 결과지를 환자에게 귀엽고 친근한 말투로 해석해줘"
    result = retrieval_qa_chain(documents, user_input)
    sentences_array = process_result(result)
    print("Sentences array:", sentences_array)
    print("\n", "data type : ", type(sentences_array))

# asyncio를 사용하여 메인 함수를 실행합니다.
asyncio.run(main())
