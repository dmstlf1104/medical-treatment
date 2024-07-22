import csv
import json
import chardet
from datetime import datetime

# CSV 파일 경로 설정
file_path = 'Hospital.csv'

# 파일을 바이너리 모드로 열어서 인코딩 확인
with open(file_path, 'rb') as f:
    raw_data = f.read()
    result = chardet.detect(raw_data)
    encoding = result['encoding']

# CSV 파일 읽기 및 데이터 객체로 저장
hospitals_list = []

with open(file_path, 'r', newline='', encoding=encoding) as csvfile:
    csvreader = csv.DictReader(csvfile)
    for row in csvreader:
        if '병원' in row['의료기관종별명'] and row['영업상태명'] != '폐업':
            lat_str = row['WGS84위도'].strip()
            lng_str = row['WGS84경도'].strip()
            
            # 위도와 경도 값이 빈 문자열이 아닌 경우만 처리
            if lat_str and lng_str:
                hospital = {
                    'name': row['사업장명'],
                    'address': row['소재지도로명주소'] if row['소재지도로명주소'] else row['소재지지번주소'],
                    'subject': row['진료과목내용'],
                    'lat': float(lat_str),
                    'lng': float(lng_str)
                }
                hospitals_list.append(hospital)

# JSON 객체 생성
data_to_save = {
    'data': hospitals_list,
    'ts': datetime.now().isoformat()
}

# JSON 파일로 저장
output_file = 'Hospital.json'
with open(output_file, 'w', encoding='utf-8') as jsonfile:
    json.dump(data_to_save, jsonfile, ensure_ascii=False, indent=4)

print(f"병원 정보 데이터를 '{output_file}'로 저장 완료")