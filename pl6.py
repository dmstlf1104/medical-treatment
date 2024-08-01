import json

# data.txt 파일 읽기
with open('data.txt', 'r', encoding='utf-8') as file:
    file_content = file.read()

# JSON 파일로 저장
with open('data.json', 'w', encoding='utf-8') as json_file:
    json_file.write(file_content)

print("data.json 파일이 생성되었습니다.")
