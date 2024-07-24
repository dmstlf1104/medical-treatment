import transformers
import torch
from transformers import AutoModelForCausalLM, BitsAndBytesConfig, AutoTokenizer
import time
model_id = "yanolja/EEVE-Korean-2.8B-v1.0"

# GPU 사용 가능 여부 확인
device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"Using device: {device}")

# 4비트 양자화 설정
quantization_config = BitsAndBytesConfig(load_in_4bit=True)

# 모델 로드 (4비트 양자화 적용)
model = AutoModelForCausalLM.from_pretrained(
    model_id,
    quantization_config=quantization_config,
    device_map="auto",
    torch_dtype=torch.float16  # bfloat16 대신 float16 사용
)

# 토크나이저 로드
tokenizer = AutoTokenizer.from_pretrained(model_id)

# 파이프라인 설정
pipeline = transformers.pipeline(
    "text-generation",
    model=model,
    tokenizer=tokenizer,
    device_map="auto",
)

# 모델을 평가 모드로 설정
pipeline.model.eval()

PROMPT = '''Your name is "힐리", helping users with their medical concerns. The assistant gives helpful, detailed, proffesional and polite answers to the user's questions.'''
instruction = input("증상을 말해주세요 ! : ")
# instruction = "심각하게 목에 통증이 있는데 어느 병원으로 가야해?"

messages = [
    {"role": "system", "content": f"{PROMPT}"},
    {"role": "user", "content": f"{instruction}"}
]

prompt = pipeline.tokenizer.apply_chat_template(
    messages, 
    tokenize=False, 
    add_generation_prompt=True
)

terminators = [
    pipeline.tokenizer.eos_token_id,
    pipeline.tokenizer.convert_tokens_to_ids("<|eot_id|>")
]
# 시간 측정 시작
start_time = time.time()

outputs = pipeline(
    prompt,
    max_new_tokens=400,
    eos_token_id=terminators,
    do_sample=True,
    temperature=0.7,
    top_p=0.92,
    top_k=50,
    num_return_sequences=1,
    repetition_penalty=5,  # 반복 패널티 추가
    no_repeat_ngram_size=3,
    num_beams = 2, # 3-gram 반복 방지
    early_stopping=True  # 조기 중단 활성화
)

# 시간 측정 종료
end_time = time.time()

# 경과 시간 계산
elapsed_time = end_time - start_time

print(f"답변 생성 시간: {elapsed_time:.2f}초")
print(outputs[0]["generated_text"][len(prompt):])
print(outputs[0]["generated_text"][len(prompt):])