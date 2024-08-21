import warnings
warnings.filterwarnings("ignore", category=FutureWarning)

from fastapi import FastAPI, WebSocket
from fastapi.responses import HTMLResponse
import cv2
import numpy as np
import mysql.connector
import insightface
from sklearn.metrics.pairwise import cosine_similarity

app = FastAPI()

# 데이터베이스 연결 설정
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',
    'database': 'testdb'
}

# 전역 변수
embedding_cache = {}  # 임베딩 캐시
face_model = None  # InsightFace 모델

def initialize_face_model():
    global face_model
    face_model = insightface.app.FaceAnalysis(name='buffalo_l')
    face_model.prepare(ctx_id=0, det_size=(640, 640))

def load_embeddings():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(buffered=True)
        cursor.execute("SELECT idx, embedding FROM userface")
        for (idx, embedding_blob) in cursor:
            embedding_cache[idx] = np.frombuffer(embedding_blob, dtype=np.float32)
        print(f"임베딩 {len(embedding_cache)}개 로드 완료")
    except mysql.connector.Error as error:
        print(f"MySQL 오류: {error}")
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()

def find_similar_face(target_embedding, threshold=0.6):
    max_similarity = -1
    most_similar_idx = None
    for idx, db_embedding in embedding_cache.items():
        similarity = cosine_similarity(target_embedding.reshape(1, -1), db_embedding.reshape(1, -1))[0][0]
        if similarity > max_similarity:
            max_similarity = similarity
            most_similar_idx = idx
    return most_similar_idx if max_similarity > threshold else None

def get_largest_face(faces):
    return max(faces, key=lambda face: (face.bbox[2] - face.bbox[0]) * (face.bbox[3] - face.bbox[1]), default=None)

def process_frame(frame):
    faces = face_model.get(frame)
    largest_face = get_largest_face(faces)
    if largest_face:
        bbox = largest_face.bbox.astype(int)
        similar_idx = find_similar_face(largest_face.embedding, threshold=0.7)
        cv2.rectangle(frame, (bbox[0], bbox[1]), (bbox[2], bbox[3]), (0, 255, 0), 2)
        if similar_idx is not None:
            text = f"ID: {similar_idx}"
            color = (0, 255, 0)  # 녹색 (매치)
        else:
            text = "Unknown"
            color = (0, 0, 255)  # 빨간색 (미매치)
        cv2.putText(frame, text, (bbox[0], bbox[1] - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.9, color, 2)

        return frame, similar_idx
    return frame, None

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    try:
        while True:
            data = await websocket.receive_bytes()
            
            # bytes를 numpy array로 변환
            nparr = np.frombuffer(data, np.uint8)
            
            # numpy array를 RGB 이미지로 디코딩
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            # 얼굴 인식 처리
            processed_frame, similar_idx = process_frame(frame)

            # 처리 결과를 클라이언트에게 전송
            await websocket.send_text(str(similar_idx) if similar_idx is not None else "")

    except Exception as e:
        print(f"Error: {e}")
    finally:
        cv2.destroyAllWindows()  # 모든 창 닫기

if __name__ == "__main__":
    initialize_face_model()
    load_embeddings()
    import uvicorn
    uvicorn.run(app, host="192.168.247.41", port=8000)