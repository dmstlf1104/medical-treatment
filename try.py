import os
from langchain.llms import Ollama
from langchain_core.prompts import ChatPromptTemplate
import torch
import streamlit as st
import fitz  # PyMuPDF
from langchain.schema import Document
import time
import requests
import re

llm = Ollama(model="bnksys/yanolja-eeve-korean-instruct-10.8b:latest")


prompt = ChatPromptTemplate.from_messages([
    ("system", "당신은 AI 의료 어시스턴트 힐리입니다. 다음 의료 보고서를 보고 아무것도 모르는 환자들이 이해하기 쉽고 귀엽게 대답해주세요. 예를 들어 결과는 ---- 나왔습니다. 자세한 상담은 의사 선생님과 하시길 바라요!. 의료 보고서:"),
    ("user", "{input}")
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

# Streamlit interface
user_input = "병원에서 받은 검사 결과지를 해석해줘 "
result = retrieval_qa_chain(user_input)
print("user_input:", result)