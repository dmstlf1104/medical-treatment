import json

# JSON 파일을 딕셔너리에 로드
with open('CH_hospitals1.json', 'r', encoding='utf-8') as file:
    data = json.load(file)

# 새로운 딕셔너리에 'data' 키로 저장
data_dict = {'data': data}

# 딕셔너리를 다시 JSON 파일로 저장
with open('CH_hospitals1_updated.json', 'w', encoding='utf-8') as file:
    json.dump(data_dict, file, ensure_ascii=False, indent=4)

print("CH_hospitals1.json 파일을 'data' 딕셔너리에 넣어 CH_hospitals1_updated.json 파일로 저장했습니다.")
