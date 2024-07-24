import easyocr
from PIL import Image

def input_image():
    
    # 이미지 열기
    image_path = 'example.jpg'
    image = Image.open(image_path)

    # EasyOCR 객체 생성
    reader = easyocr.Reader(['en', 'ko'])  # 영어와 한국어 인식 설정

    # OCR 수행
    results = reader.readtext(image)

    # 결과 출력
    for (bbox, text, prob) in results:
        print(f"Text: {text}, Probability: {prob:.2f}")
        print(f"Bbox: {bbox}")
