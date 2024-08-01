import json

# 파일 읽기 함수
def read_json_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        return json.load(file)

# 파일 쓰기 함수
def write_json_file(file_path, data):
    with open(file_path, 'w', encoding='utf-8') as file:
        json.dump(data, file, indent=4, ensure_ascii=False)

# 데이터 파일 읽기
merged_hospitals = read_json_file('merged_hospitals_updated.json')
new_data = read_json_file('data.json')

# 병원 이름 집합 생성 (이미 존재하는 병원들)
existing_names = {hospital['name'] for hospital in merged_hospitals['data']}

# 새로운 병원 데이터 추가
for entry in new_data['data']:
    if entry['name'] not in existing_names:
        merged_hospitals['data'].append(entry)

# 업데이트된 데이터 저장
write_json_file('merged_hospitals_updated.json', merged_hospitals)

print("merged_hospitals_updated.json 파일이 업데이트되었습니다.")
