import csv
import sqlite3
import chardet

# CSV 파일 경로 설정
file_path = 'C:/Users/admin/Documents/GitHub/medical-treatment/Hospital.csv'

# 파일을 바이너리 모드로 열어서 인코딩 확인
with open(file_path, 'rb') as f:
    raw_data = f.read()
    result = chardet.detect(raw_data)
    encoding = result['encoding']

# SQLite 데이터베이스 파일 경로
db_file = 'hospitals.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# CSV 파일 읽기 및 데이터베이스에 삽입
with open(file_path, 'r', newline='', encoding=encoding) as csvfile:
    csvreader = csv.DictReader(csvfile)
    for row in csvreader:
        # 영업상태명 필드 값 확인
        operating_status = row['영업상태명']
        
        # 폐업인 경우 해당 데이터는 삽입하지 않음
        if operating_status == '폐업':
            continue
        
        # 데이터 삽입
        Hospital_name = row['사업장명']
        company_name = row['소재지도로명주소']
        medical_subject = row['진료과목내용']
        
        # WGS84위도, 경도 데이터가 빈 문자열이면 None 처리
        latitude = float(row['WGS84위도']) if row['WGS84위도'].strip() else None
        longitude = float(row['WGS84경도']) if row['WGS84경도'].strip() else None
        
        # 적절한 SQL 쿼리 작성하여 데이터베이스에 삽입
        cursor.execute("INSERT INTO hospitals (city_name, company_name, medical_subject, latitude, longitude) VALUES (?, ?, ?, ?, ?)",
                       (Hospital_name, company_name, medical_subject, latitude, longitude))

# 변경사항 저장 및 연결 종료
conn.commit()
conn.close()
