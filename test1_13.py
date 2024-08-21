from fastapi import FastAPI, WebSocket
import cv2
import numpy as np
from insightface.app import FaceAnalysis
import mysql.connector
import base64

app = FastAPI()

# FaceAnalysis 앱 초기화
face_app = FaceAnalysis(name='buffalo_l')
face_app.prepare(ctx_id=0, det_size=(640, 640))

# MySQL 연결 설정
db_config = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'medical_records_db'
}

def get_largest_face_embedding(image):
    faces = face_app.get(image)
    if len(faces) > 0:
        # 가장 큰 얼굴 찾기
        largest_face = max(faces, key=lambda face: (face.bbox[2] - face.bbox[0]) * (face.bbox[3] - face.bbox[1]))
        return largest_face.embedding
    return None

def save_embedding_to_mysql(user_id, embedding):
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        embedding_bytes = embedding.tobytes()

        query = "INSERT INTO userface (user_id, embedding) VALUES (%s, %s)"
        cursor.execute(query, (user_id, embedding_bytes))

        idx = cursor.lastrowid
        conn.commit()
        print(f"임베딩이 성공적으로 저장되었습니다. (idx: {idx}, user_id: {user_id})")
        return idx

    except mysql.connector.Error as error:
        print(f"MySQL에 임베딩을 저장하는 중 오류 발생: {error}")
        return None

    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        data = await websocket.receive_text()
        
        # 데이터 파싱
        user_id, image_base64 = data.split(',', 1)
        
        # base64 디코딩
        image_bytes = base64.b64decode(image_base64)
        
        # bytes를 numpy array로 변환
        nparr = np.frombuffer(image_bytes, np.uint8)
        
        # numpy array를 RGB 이미지로 디코딩
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        embedding = get_largest_face_embedding(frame)
        if embedding is not None:
            idx = save_embedding_to_mysql(user_id, embedding)
            if idx is not None:
                await websocket.send_text(f"Success: Embedding saved with idx {idx} for user {user_id}")
            else:
                await websocket.send_text("Error: Failed to save embedding")
        else:
            await websocket.send_text("Error: No face detected")

    except Exception as e:
        print(f"WebSocket error: {e}")
        await websocket.send_text(f"Error: {str(e)}")
    finally:
        await websocket.close()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="192.168.247.41", port=8001)