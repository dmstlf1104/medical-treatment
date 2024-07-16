import sqlite3

# SQLite 데이터베이스 파일 경로
db_file = 'hospitals.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# hospitals 테이블 생성
cursor.execute('''
    CREATE TABLE IF NOT EXISTS hospitals (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        city_name TEXT,
        company_name TEXT,
        medical_subject TEXT,
        latitude REAL,
        longitude REAL
    )
''')

# 변경사항 저장 및 연결 종료
conn.commit()
conn.close()

print("SQLite 데이터베이스 및 테이블 생성 완료: hospitals.db")
