import json

# JSON 파일 읽기 (UTF-8 인코딩 지정)
with open('merged_hospitals_updated.json', 'r', encoding='utf-8') as file:
    try:
        content = json.load(file)
    except json.JSONDecodeError as e:
        print(f"JSON decode error: {e}")
        content = {}

# 데이터가 data 딕셔너리 안에 리스트로 저장되어 있는지 확인
data = content.get('data', [])

# time이 null인 항목의 갯수
null_time_count = sum(1 for item in data if isinstance(item, dict) and item.get('time') is None)

# name의 갯수
name_count = sum(1 for item in data if isinstance(item, dict) and 'name' in item)

print(f"Null time count: {null_time_count}")
print(f"Name count: {name_count}")
