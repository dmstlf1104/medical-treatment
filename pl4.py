import json

# Define the file path
file_path = 'merged_hospitals_updated.json'

# Load the JSON file
with open(file_path, 'r', encoding='utf-8') as file:
    data = json.load(file)

# Check for 'time' key in each item within the 'data' dictionary
for item in data['data']:
    if 'time' not in item:
        item['time'] = None

# Save the modified data back to the JSON file
with open(file_path, 'w', encoding='utf-8') as file:
    json.dump(data, file, ensure_ascii=False, indent=4)

print("The file has been updated successfully.")
