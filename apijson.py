import sqlite3
import json

# SQLite 데이터베이스 파일 경로
db_file = 'hospitals.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# 쿼리 실행 및 데이터 가져오기
cursor.execute("SELECT * FROM hospitals")
rows = cursor.fetchall()

# 결과를 JSON 형식으로 변환
data = []
for row in rows:
    hospital = {
        'id': row[0],
        'city_name': row[1],
        'company_name': row[2],
        'medical_subject': row[3],
        'latitude': row[4],
        'longitude': row[5]
    }
    data.append(hospital)

# JSON 파일로 저장
json_file = 'hospitals.json'
with open(json_file, 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=4)

# 연결 종료
conn.close()

print(f"데이터가 {json_file} 파일로 성공적으로 저장되었습니다.")

