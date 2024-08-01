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
new_data = read_json_file('CH_hospitals2.json')

# 업데이트할 데이터 매핑 생성
update_mapping = {entry['name']: entry for entry in new_data['data']}

# 데이터 업데이트
for hospital in merged_hospitals['data']:
    name = hospital['name']
    if name in update_mapping:
        updated_info = update_mapping[name]
        hospital['lat'] = updated_info.get('lat', hospital.get('lat'))
        hospital['lng'] = updated_info.get('lng', hospital.get('lng'))
        hospital['phone'] = updated_info.get('phone', hospital.get('phone'))
        hospital['time'] = updated_info.get('time', hospital.get('time'))

# 업데이트된 데이터 저장
write_json_file('merged_hospitals_updated.json', merged_hospitals)

print("merged_hospitals_updated.json 파일이 업데이트되었습니다.")
