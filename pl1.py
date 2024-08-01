import json

# Load the JSON data from the file with UTF-8 encoding
with open('merged_hospitals_updated.json', 'r', encoding='utf-8') as file:
    ch_hospitals1 = json.load(file)

# Function to get hospital information by name
def get_hospital_info(hospitals, hospital_name):
    for hospital in hospitals.get('data', []):
        if hospital.get('name') == hospital_name:
            return hospital
    return None

# Check if "천안자생한방병원" exists and get its information
hospital_name_to_check = "혜강병원"
hospital_info = get_hospital_info(ch_hospitals1, hospital_name_to_check)

if hospital_info:
    print(f"'{hospital_name_to_check}' exists in the data. Here is the information:")
    print(json.dumps(hospital_info, indent=4, ensure_ascii=False))
else:
    print(f"'{hospital_name_to_check}' does not exist in the data.")
