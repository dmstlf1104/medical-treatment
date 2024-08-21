from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import mysql.connector
import json

app = FastAPI()

class SearchQuery(BaseModel):
    query: str

def search_terms(search_query: str):
    result_dict = {}
    
    try:
        conn = mysql.connector.connect(
            host="localhost",
            user="tester",
            password="1234",
            database="medical_records_db"
        )
        cursor = conn.cursor()
        
        cursor.execute("SELECT term_ko, term_en, explanation FROM medical_terms")
        all_results = cursor.fetchall()
        
        search_words = search_query.split()
        
        for term_ko, term_en, explanation in all_results:
            term_ko = term_ko or ""
            term_en = term_en or ""
            
            for word in search_words:
                if term_ko in word:
                    result_dict[term_ko] = [term_ko, term_en, explanation]
                    break
                elif term_en in word:
                    result_dict[term_en] = [term_ko, term_en, explanation]
                    break
        
        return result_dict
    
    except mysql.connector.Error as e:
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()

@app.post("/search_terms")
async def api_search_terms(query: SearchQuery):
    result = search_terms(query.query)
    # print(result)
    return result

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.247.41", port=8002)