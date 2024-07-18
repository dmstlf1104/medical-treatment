import csv
import sqlite3
import chardet

# CSV 파일 경로 설정
file_path = 'ChungcheongHospital.csv'

# 파일을 바이너리 모드로 열어서 인코딩 확인
with open(file_path, 'rb') as f:
    raw_data = f.read()
    result = chardet.detect(raw_data)
    encoding = result['encoding']

# SQLite 데이터베이스 파일 경로
db_file = 'C:/Users/admin/Documents/GitHub/medical-treatment/ChungcheongHospital.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# CSV 파일 읽기 및 데이터 객체로 저장
hospitals_list = []

with open(file_path, 'r', newline='', encoding=encoding) as csvfile:
    csvreader = csv.DictReader(csvfile)
    for row in csvreader:
        if '병원' in row['의료기관종별명'] and row['영업상태명'] != '폐업':
            hospital = {
                'Hospital_name': row['사업장명'],
                'Hospital_Map': row['소재지전체주소'] if row['소재지전체주소'] else row['도로명전체주소'],
                'medical_subject': row['진료과목내용명'],
                'phone_number': row['소재지전화'],
                'latitude': float(row['좌표정보(x)']) if row['좌표정보(x)'].strip() else None,
                'longitude': float(row['좌표정보(y)']) if row['좌표정보(y)'].strip() else None
            }
            hospitals_list.append(hospital)
            # 데이터베이스에도 삽입할 수 있지만 여기서는 객체 저장에만 초점을 맞춤

# 변경사항 저장 및 연결 종료
conn.commit()
conn.close()

print("병원 정보 데이터 객체로 저장 완료")

# hospitals_list를 이후 다른 처리나 저장을 위해 사용할 수 있음
