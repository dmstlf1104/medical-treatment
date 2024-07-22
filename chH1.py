import sqlite3

# SQLite 데이터베이스 파일 경로
db_file = 'Hospital.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# hospitals 테이블 생성
cursor.execute('''
    CREATE TABLE IF NOT EXISTS hospitals (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        address TEXT,
        subject TEXT,
        lat REAL,
        lng REAL
    )
''')

# 변경사항 저장
conn.commit()

# hospitals 테이블 구조 확인
cursor.execute("PRAGMA table_info(Hospitals);")
table_info = cursor.fetchall()

# 결과 출력
for column in table_info:
    print(column)

# 연결 종료
conn.close()

print("SQLite 데이터베이스 및 테이블 생성 완료: Hospital.db")