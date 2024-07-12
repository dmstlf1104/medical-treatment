import requests

# Strapi 서버의 URL
base_url = 'http://localhost:1337'

# GET 요청을 보내어 모든 위치 정보 조회
response = requests.get(f'{base_url}/locations')

# 응답 데이터 확인
if response.status_code == 200:
    locations = response.json()  # JSON 형식의 데이터로 변환
    for location in locations:
        print(location)  # 위치 정보 출력
else:
    print(f'Error: {response.status_code}')
