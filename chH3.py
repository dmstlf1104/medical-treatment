import sqlite3

# SQLite 데이터베이스 파일 경로
db_file = 'ChungcheongHospital.db'

# SQLite 연결 설정
conn = sqlite3.connect(db_file)
cursor = conn.cursor()

# hospitals 테이블에서 모든 데이터 조회
cursor.execute('SELECT * FROM hospitals')
rows = cursor.fetchall()

# 조회 결과 출력
for row in rows:
    print(row)

# 연결 종료
conn.close()
