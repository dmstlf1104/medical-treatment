from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from mysql.connector import pooling, Error
import os
from typing import List, Dict, Any
import asyncio
from anthropic import AsyncAnthropic

# FastAPI 애플리케이션 초기화
app = FastAPI()

# 데이터베이스 연결 풀 설정
db_config = {
    'host': '192.168.247.41',
    'user': 'tester',
    'password': '1234',
    'database': 'medical_records_db',
}

# MySQL 연결 풀 생성
connection_pool = pooling.MySQLConnectionPool(
    pool_name="mypool",
    pool_size=5,
    **db_config
)

# AsyncAnthropic 클라이언트 초기화
client = AsyncAnthropic(api_key=os.environ.get("ANTHROPIC_API_KEY"))

# 사용자 입력 모델 정의
class Conversation(BaseModel):
    user_id: str
    message: str

# 데이터베이스에서 대화 기록을 가져오는 함수
def get_conversation(user_id: str, chatname: str) -> List[Dict[str, Any]]:
    try:
        connection = connection_pool.get_connection()
        cursor = connection.cursor(dictionary=True)
        
        # 주어진 user_id과 chatname에 대한 대화 기록 조회
        query = "SELECT input, output FROM chattalk WHERE user_id = %s AND chatname = %s"
        cursor.execute(query, (user_id, chatname))
        conversation = cursor.fetchall()

    except Error as e:
        print(f"Error fetching conversation: {e}")
        raise HTTPException(status_code=500, detail="Internal Server Error")
    
    finally:
        cursor.close()
        connection.close()

    return conversation

# 데이터베이스에 대화 기록을 저장하는 함수
def save_message(user_id: str, chatname: str, input_msg: str, output_msg: str):
    try:
        connection = connection_pool.get_connection()
        cursor = connection.cursor()

        # 주어진 대화 내용 삽입
        query = "INSERT INTO chattalk (user_id, chatname, input, output) VALUES (%s, %s, %s, %s)"
        cursor.execute(query, (user_id, chatname, input_msg, output_msg))
        connection.commit()

    except Error as e:
        print(f"Error saving message: {e}")
        raise HTTPException(status_code=500, detail="Internal Server Error")
    
    finally:
        cursor.close()
        connection.close()

# 스트리밍 응답을 생성하는 함수
async def stream_response(user_id: str, prompt: str):
    chatname = prompt[:5]
    conversation = get_conversation(user_id, chatname) or []

    system_prompt = """You are an AI assistant named 'Healy'. You have a friendly and cheerful personality,
    always striving to help. Please respond to user questions in Korean clearly, and in an easy-to-understand manner.
    Add explanations when necessary, but keep the entire response within 200 characters."""

    messages = []
    for msg in conversation:
        messages.append({"role": "user", "content": msg['input']})
        messages.append({"role": "assistant", "content": msg['output']})
    messages.append({"role": "user", "content": prompt})

    # Claude API 호출
    stream = await client.messages.create(
        model="claude-3-sonnet-20240229",
        max_tokens=300,
        messages=messages,
        system=system_prompt,
        stream=True
    )
    
    full_response = ""
    async for chunk in stream:
        if hasattr(chunk, 'content'):  # 'content' 속성이 있는지 확인
            content = chunk.content
            if content:
                full_response += content
                yield f"data: {content}\n\n"
        elif hasattr(chunk, 'delta') and hasattr(chunk.delta, 'text'):  # 이전 버전 호환성을 위해 유지
            content = chunk.delta.text
            if content:
                full_response += content
                yield f"data: {content}\n\n"

    yield f"data: [DONE]\n\n"

    # 대화 기록에 사용자 메시지와 AI 응답 저장
    save_message(user_id, chatname, prompt, full_response)

# /generate 엔드포인트 정의
@app.post("/generate")
async def generate(conversation: Conversation):
    print(f"사용자 입력: {conversation.message}")
    print("응답 생성 중...")

    return StreamingResponse(
        stream_response(conversation.user_id, conversation.message),
        media_type="text/event-stream"
    )

# 애플리케이션 실행
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)