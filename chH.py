import csv
import chardet

# CSV 파일 경로 설정
file_path = 'Busan.csv'

# 파일을 바이너리 모드로 열어서 인코딩 확인
with open(file_path, 'rb') as f:
    raw_data = f.read()
    result = chardet.detect(raw_data)
    encoding = result['encoding']

# CSV 파일 열기
with open(file_path, 'r', newline='', encoding=encoding) as csvfile:
    csvreader = csv.reader(csvfile)
    for row in csvreader:
        print(row)
