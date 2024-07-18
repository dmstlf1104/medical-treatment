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

# CSV 파일 읽기 및 데이터베이스에 삽입
with open(file_path, 'r', newline='', encoding=encoding) as csvfile:
    csvreader = csv.DictReader(csvfile)
    for row in csvreader:
        # 의료기관 종별명이 '병원'인 경우만 처리
        if '병원' in row['의료기관종별명']:
            # 영업상태명 필드 값 확인
            operating_status = row['영업상태명']
            
            # 폐업인 경우 해당 데이터는 삽입하지 않음
            if operating_status == '폐업':
                continue
            
            # 데이터 삽입
            Hospital_name = row['사업장명']
            
            # 소재지전체주소 또는 도로명전체주소 사용
            if row['소재지전체주소']:
                Hospital_Map = row['소재지전체주소']
            else:
                Hospital_Map = row['도로명전체주소']
            
            medical_subject = row['진료과목내용명']
            phone_number = row['소재지전화']
            
            # WGS84위도, 경도 데이터가 빈 문자열이면 None 처리
            latitude = float(row['좌표정보(x)']) if row['좌표정보(x)'].strip() else None
            longitude = float(row['좌표정보(y)']) if row['좌표정보(y)'].strip() else None
            
            # 데이터베이스에 삽입
            cursor.execute("INSERT INTO hospitals (Hospital_name, Hospital_Map, medical_subject, phone_number, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)",
                           (Hospital_name, Hospital_Map, medical_subject, phone_number, latitude, longitude))

# 변경사항 저장 및 연결 종료
conn.commit()
conn.close()

print("병원 정보 데이터 삽입 완료")