import json

# JSON 파일 읽기 함수
def read_json(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        return json.load(f)

# JSON 파일 쓰기 함수
def write_json(data, filename):
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

# JSON 파일 경로
file1 = 'Hospital.json'
file2 = 'hospitals_data.json'
output_file = 'merged_hospitals.json'

# JSON 파일 읽기
data1 = read_json(file1)
data2 = read_json(file2)

# 데이터 병합
merged_data = {
    "data": data1["data"] + data2["data"],
    "ts": max(data1["ts"], data2["ts"])  # 최신 타임스탬프 선택
}

# 병합된 데이터 JSON 파일로 저장
write_json(merged_data, output_file)

print(f'Merged JSON saved to {output_file}')
