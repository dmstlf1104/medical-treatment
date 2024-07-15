import sqlite3
import re

def normalize_term(term):
    # 모든 공백 제거 및 소문자 변환
    return re.sub(r'\s+', '', term).lower()

def search_terms(search_query):
    conn = sqlite3.connect('medical_terms2.db')
    cursor = conn.cursor()
    
    # 정규화된 검색어 생성
    normalized_query = normalize_term(search_query)
    
    # 모든 term을 가져옴
    cursor.execute("SELECT term, explanation FROM terms")
    all_results = cursor.fetchall()
    
    conn.close()
    
    # 정규화된 term과 비교하여 결과 필터링
    filtered_results = [
        (term, explanation) for term, explanation in all_results
        if normalized_query in normalize_term(term)
    ]
    
    # 중복 제거
    unique_results = {}
    for term, explanation in filtered_results:
        if term not in unique_results:
            unique_results[term] = explanation
    
    return list(unique_results.items())

def display_results(results):
    if not results:
        print("검색 결과가 없습니다.")
        return None
    
    if len(results) == 1:
        return results[0]
    
    print("\n검색 결과:")
    for i, (term, _) in enumerate(results, 1):
        print(f"{i}. {term}")
    
    while True:
        try:
            choice = int(input("\n자세히 볼 항목의 번호를 입력하세요 (0: 취소): "))
            if choice == 0:
                return None
            if 1 <= choice <= len(results):
                return results[choice-1]
            else:
                print("올바른 번호를 입력해주세요.")
        except ValueError:
            print("숫자를 입력해주세요.")

def main():
    while True:
        search_query = input("검색할 의학 용어를 입력하세요 (종료하려면 'q' 입력): ")
        
        if search_query.lower() == 'q':
            print("프로그램을 종료합니다.")
            break
        
        results = search_terms(search_query)
        
        selected_result = display_results(results)
        
        if selected_result:
            term, explanation = selected_result
            print(f"\n{term}에 대한 설명:")
            print(explanation)
        
        print()  # 빈 줄 출력

if __name__ == "__main__":
    main()