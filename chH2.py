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
db_file = 'ChungcheongHospital.db'

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
            
            # WGS84 위도와 경도 데이터가 빈 문자열이면 해당 병원 데이터 삽입하지 않음
            lat_str = row['좌표정보(x)'].strip()
            lng_str = row['좌표정보(y)'].strip()
            
            if lat_str == '' or lng_str == '':
                continue
            
            lat = float(lat_str)
            lng = float(lng_str)
            
            # 데이터 삽입
            name = row['사업장명']
            
            # 소재지전체주소 또는 도로명전체주소 사용
            if row['소재지전체주소']:
                address = row['소재지전체주소']
            else:
                address = row['도로명전체주소']
            
            subject = row['진료과목내용명']
            phone = row['소재지전화']
            
            # 데이터베이스에 삽입
            cursor.execute("INSERT INTO Hospitals (Name, Address, Subject, Phone, Lat, Lng) VALUES (?, ?, ?, ?, ?, ?)",
                           (name, address, subject, phone, lat, lng))

# 변경사항 저장 및 연결 종료
conn.commit()
conn.close()

print("병원 정보 데이터 삽입 완료")
