import json

# JSON 파일을 UTF-8 인코딩으로 로드
with open('merged_hospitals_updated.json', 'r', encoding='utf-8') as file:
    merged_data = json.load(file)

with open('CH_hospitals1.json', 'r', encoding='utf-8') as file:
    ch_data = json.load(file)

# 데이터는 'data' 키 안에 저장되어 있음
merged_hospitals = merged_data['data']
ch_hospitals = ch_data['data']

# CH_hospitals 데이터를 name을 key로 하는 dictionary로 변환
ch_hospitals_dict = {hospital['name']: hospital for hospital in ch_hospitals}

# time 값과 lat, lng 값 업데이트
for hospital in merged_hospitals:
    hospital_name = hospital['name']
    if hospital_name in ch_hospitals_dict:
        ch_hospital = ch_hospitals_dict[hospital_name]
        if 'time' not in hospital or not hospital['time']:
            hospital['time'] = ch_hospital.get('time')
        hospital['lat'] = ch_hospital.get('lat', hospital.get('lat'))
        hospital['lng'] = ch_hospital.get('lng', hospital.get('lng'))

# 변경된 데이터를 UTF-8 인코딩으로 저장
merged_data['data'] = merged_hospitals  # 원본 구조를 유지
with open('merged_hospitals_updated_1.json', 'w', encoding='utf-8') as file:
    json.dump(merged_data, file, ensure_ascii=False, indent=4)

print("업데이트가 완료되었습니다. 'merged_hospitals_updated.json' 파일을 확인하세요.")
