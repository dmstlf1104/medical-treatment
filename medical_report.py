import os
import requests
import re
from anthropic import AsyncAnthropic

# 텍스트 전처리 함수
def preprocess_text(text):
    text = re.sub(r'\s+', ' ', text).strip()
    text = re.sub(r'[^\w\s.,?]', '', text)
    text = re.sub(r'\n+', '\n', text)
    text = '. '.join(s.capitalize() for s in text.split('. '))
    return text

# 파일에서 텍스트를 가져오는 함수
def fetch_text_from_file(filename, api_key):
    url = "https://api.upstage.ai/v1/document-ai/ocr"
    headers = {"Authorization": f"Bearer {api_key}"}
    
    with open(filename, "rb") as file:
        files = {"document": file}
        response = requests.post(url, headers=headers, files=files)
    
    if response.status_code == 200:
        result = response.json()
        text = result.get('text', '')
        return preprocess_text(text)
    else:
        print(f"Error: {response.status_code}")
        print(response.content)
        return "OCR 추출에 실패했습니다."

# 설명을 생성하는 비동기 함수
async def generate_explanation(client, documents):
    system_prompt = "당신은 의사로서, 환자의 검사 결과를 해석하고 요약하는 역할을 합니다. 환자에게 검사 종류에 대해 간단히 설명한 뒤 검사 결과를 간단히 요약하고, 주요 발견사항과 그 의미를 설명해주세요."
    
    messages = [{"role": "user", "content": f"다음 검사 결과를 해석하고 환자에게 친절히 설명해주세요: {documents}"}]
    
    response = await client.messages.create(
        model="claude-3-sonnet-20240229",
        max_tokens=500,
        system=system_prompt,
        messages=messages
    )
    
    if hasattr(response, 'content') and response.content:
        return response.content[0].text
    else:
        print(f"Unexpected response format: {response}")
        return "설명 생성에 실패했습니다."

# 메인 함수
async def main():
    api_key = os.environ.get("UPSTAGE_API_KEY")
    anthropic_api_key = os.environ.get("ANTHROPIC_API_KEY")
    
    if not api_key or not anthropic_api_key:
        print("API 키가 설정되지 않았습니다.")
        return
    
    filename = input("파일을 입력하세요 : ")
    
    documents = fetch_text_from_file(filename, api_key)
    
    if documents.startswith("OCR 추출에 실패했습니다."):
        print(documents)
        return

    async with AsyncAnthropic(api_key=anthropic_api_key) as client:
        explanation = await generate_explanation(client, documents)
        print("\n검사 결과 해석 및 요약:\n", explanation)

# 비동기 함수 실행
if __name__ == "__main__":
    import asyncio
    asyncio.run(main())