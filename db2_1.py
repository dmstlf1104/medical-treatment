import sqlite3

# SQLite 데이터베이스 파일 경로
db_file = 'hospitals.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# hospitals 테이블 구조 확인
cursor.execute("PRAGMA table_info(hospitals);")
table_info = cursor.fetchall()

# 결과 출력
for column in table_info:
    print(column)

# 연결 종료
conn.close()
