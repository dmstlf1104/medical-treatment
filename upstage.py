import requests

api_key = "up_8sqFsKHvhK5EqZeiO8p8DfjXG2yZ7"
filename = "exp3.jpg"

url = "https://api.upstage.ai/v1/document-ai/ocr"
headers = {"Authorization": f"Bearer {api_key}"}

with open(filename, "rb") as file:
    files = {"document": file}
    response = requests.post(url, headers=headers, files=files)
    
if response.status_code == 200:
    result = response.json()
    # JSON 구조에 따라 텍스트 추출
    # 이 부분은 JSON 응답 구조에 따라 조정 필요
    # 예를 들어, result['text'] 또는 result['data']['text'] 등
    text = result.get('text', '')  # 'text' 키에 해당하는 값 추출
    print(text)
else:
    print(f"Error: {response.status_code}")
    print(response.content)
    


