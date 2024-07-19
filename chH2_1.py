import csv
import sqlite3
import chardet

# CSV 파일 경로 설정
file_path = 'Busan.csv'

# 파일을 바이너리 모드로 열어서 인코딩 확인
with open(file_path, 'rb') as f:
    raw_data = f.read()
    result = chardet.detect(raw_data)
    encoding = result['encoding']

# SQLite 데이터베이스 파일 경로
db_file = 'Busan.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# CSV 파일 읽기 및 데이터베이스에 삽입
with open(file_path, 'r', newline='', encoding=encoding) as csvfile:
    csvreader = csv.DictReader(csvfile)
    for row in csvreader:
        # WGS84 위도와 경도 데이터가 빈 문자열이면 해당 병원 데이터 삽입하지 않음
        lat_str = row['위도'].strip()
        lng_str = row['경도'].strip()
        
        if lat_str == '' or lng_str == '':
            continue
        
        lat = float(lat_str)
        lng = float(lng_str)
        
        # 데이터 삽입
        name = row['의료기관명']
        address = row['도로명주소']
        
        # 데이터베이스에 삽입
        cursor.execute("INSERT INTO Hospitals (Name, Address, Lat, Lng) VALUES (?, ?, ?, ?)",
                       (name, address, lat, lng))

# 변경사항 저장 및 연결 종료
conn.commit()
conn.close()

print("병원 정보 데이터 삽입 완료")
