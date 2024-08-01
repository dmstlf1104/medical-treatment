import mysql.connector

# MySQL 연결 설정
db_config = {
    'host': ' 192.168.247.41',
    'user': 'tester',
    'password': '1234',
    'database': 'medical_records_db',
}

# OCR 텍스트와 분석 결과 (예시 데이터)
ocr_text = "이것은 OCR로 인식된 텍스트입니다."
analysis_result = "이것은 분석 결과입니다."

try:
    # MySQL 연결
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()

    # SQL 쿼리 준비
    query = "INSERT INTO medical_records (ocr_text, analysis_result) VALUES (%s, %s)"
    values = (ocr_text, analysis_result)

    # 쿼리 실행
    cursor.execute(query, values)

    # 변경사항 커밋
    conn.commit()

    print("데이터가 성공적으로 삽입되었습니다.")

except mysql.connector.Error as error:
    print(f"데이터 삽입 중 오류 발생: {error}")

finally:
    # 연결 종료
    if conn.is_connected():
        cursor.close()
        conn.close()
        print("MySQL 연결이 종료되었습니다.")