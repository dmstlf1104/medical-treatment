import mysql.connector

# 데이터베이스 연결 정보
config = {
    'host': '192.168.1.5',
    'user': 'tester',
    'password': '1234',
    'database': 'medical_records_db',
}

try:
    # 데이터베이스 연결
    connection = mysql.connector.connect(**config)
    cursor = connection.cursor()

    # 테이블 생성
    create_table_query = """
    CREATE TABLE IF NOT EXISTS employees (
        id INT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(100),
        email VARCHAR(100),
        department VARCHAR(100),
        salary DECIMAL(10, 2)
    )
    """
    cursor.execute(create_table_query)
    print("테이블이 성공적으로 생성되었습니다.")

    # 데이터 삽입
    insert_data_query = """
    INSERT INTO employees (name, email, department, salary)
    VALUES (%s, %s, %s, %s)
    """
    employee_data = [
        ("김철수", "kim@example.com", "개발", 5000000),
        ("이영희", "lee@example.com", "마케팅", 4500000),
        ("박지훈", "park@example.com", "인사", 4800000),
    ]

    cursor.executemany(insert_data_query, employee_data)
    connection.commit()
    print(f"{cursor.rowcount}개의 레코드가 삽입되었습니다.")

    # 데이터 조회
    select_query = "SELECT * FROM employees"
    cursor.execute(select_query)
    results = cursor.fetchall()

    print("\n삽입된 데이터:")
    for row in results:
        print(row)

except mysql.connector.Error as error:
    print(f"오류 발생: {error}")

finally:
    if connection.is_connected():
        cursor.close()
        connection.close()
        print("MySQL 연결이 닫혔습니다.")