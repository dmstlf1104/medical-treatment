import requests
import uuid
import time
import json

# Naver Cloud에서 발급받은 secret_key
secret_key = 'Zk56dFRYbHlJY0JRd0xtYVFWYnV2SGpaUW1Uc0pNVno='

# API URL
api_url = 'https://pg371d7zjc.apigw.ntruss.com/custom/v1/32552/735b76b30cedecfca71465cd187dd8dab363e2a5e987f502ad9581a8738ff988/general'

# OCR을 수행할 이미지 파일
image_file = 'exp.jpg'

# 요청 JSON 데이터 생성
request_json = {
    'images': [
        {
            'format': 'jpg',
            'name': 'demo'
        }
    ],
    'requestId': str(uuid.uuid4()),
    'version': 'V2',
    'timestamp': int(round(time.time() * 1000))
}

# 헤더 설정
headers = {
  'X-OCR-SECRET': secret_key
}

# 파일 및 데이터 준비
files = {
  'file': ('example.jpg', open(image_file,'rb'), 'image/jpeg'),
  'message': (None, json.dumps(request_json), 'application/json')
}

# POST 요청 보내기
response = requests.post(api_url, headers=headers, files=files)

# 결과 확인 및 텍스트 추출
if response.status_code == 200:
    result = response.json()
    for image in result['images']:
        print(f"Image: {image['name']}")
        for field in image['fields']:
            print(f"Extracted Text: {field['inferText']}")
else:
    print(f"Error occurred: {response.status_code}, {response.text}")