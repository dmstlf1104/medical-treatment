### 최종최종

from langchain.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
from langchain.schema import Document
import time
import requests
import re

llm = Ollama(model="GRACE-model:latest",
             num_ctx = 1024,
             temperature = 0.4)


prompt = ChatPromptTemplate.from_messages([
    ("system", """"당신은 AI 의료 어시스턴트 힐리(HEALY)입니다. 환자의 검사 결과를 다음과 같은 형식으로 친절하고 명확하게 설명해 주세요:

1. 먼저 인사와 함께 검사 결과를 설명할 것임을 알리세요.
2. 결과 해석은 의료 전문가와 상담해야 함을 강조하세요.
3. '- 검사 결과 요약' 제목 아래 각 검사 결과를 bullet point(-)로 나열하세요.
4. 각 bullet point는 간단하고 이해하기 쉬운 언어로 작성하세요.
5. 마지막에 '- 결론 ---' 제목을 추가하고, 의료 전문가와의 상담 필요성을 다시 한 번 강조하세요.

전문 용어는 가능한 쉬운 말로 풀어 설명하고, 환자가 이해하기 쉽도록 해주세요."""),
    ("user", "환자님의 검사 결과를 설명해 드리겠습니다. 이 내용을 바탕으로 치료 결정을 내려선 안 되며, 반드시 의료 전문가와 상담하셔야 합니다. 검사 결과 : {input}")
])


api_key = "up_8sqFsKHvhK5EqZeiO8p8DfjXG2yZ7"
filename = input("파일을 입력하세요 : ")

url = "https://api.upstage.ai/v1/document-ai/ocr"
headers = {"Authorization": f"Bearer {api_key}"}

def preprocess_text(text):
    # 불필요한 공백 제거
    text = re.sub(r'\s+', ' ', text).strip()
    
    # 특수 문자 제거 (단, 마침표, 쉼표, 물음표는 유지)
    text = re.sub(r'[^\w\s.,?]', '', text)
    
    # 연속된 줄바꿈을 하나의 줄바꿈으로 대체
    text = re.sub(r'\n+', '\n', text)
    
    # 문장 시작 시 소문자를 대문자로 변경
    text = '. '.join(s.capitalize() for s in text.split('. '))
    
    return text

with open(filename, "rb") as file:
    files = {"document": file}
    response = requests.post(url, headers=headers, files=files)

if response.status_code == 200:
    result = response.json()
    text = result.get('text', '')
    documents = preprocess_text(text)
else:
    print(f"Error: {response.status_code}")
    print(response.content)
    documents = "OCR 추출에 실패했습니다."

# Create RAG chain manually using FAISS
def retrieval_qa_chain(input_text):
    # Retrieve relevant documents
     
    # Log retrieved documents
    # print("Retrieved documents:", documents)
    
    # Create prompt with retrieved documents
    full_prompt = prompt.format(input=documents+ "/n" +input_text)
    
    # Log full prompt
    # print("Full prompt:", full_prompt)
    
    # Generate response using the language model
    start_time = time.time()
    response = llm(full_prompt)
    end_time = time.time()
    
    # Log response
    # print("Response:", response)
    print(f"Prediction time: {end_time - start_time:.2f} seconds")
    return response

user_input = "병원에서 받은 검사 결과지를 환자에게 친근한 말투로 해석해줘 "
result = retrieval_qa_chain(user_input)
print("user_input:", result)
print("\n", "data type : ", type(result))