# 1. Python 3.9 Slim 베이스 이미지 사용
FROM python:3.9-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 시스템 의존성 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    cmake \
    make \
    g++ \
    build-essential \
    libgl1-mesa-glx \
    libx11-dev \
    libatlas-base-dev \
    libjpeg-dev \
    libpng-dev \
    libtiff-dev \
    libboost-all-dev \
    libopenblas-dev \
    python3-dev \
    python3-setuptools \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    && rm -rf /var/lib/apt/lists/*

# 4. requirements.txt 복사
COPY requirements.txt .

# 5. pip 업그레이드 및 numpy 먼저 설치 (dlib 의존성 해결)
RUN pip install --upgrade pip && \
    pip install --no-cache-dir numpy

# 6. dlib 먼저 설치 (컴파일 필요)
RUN pip install --no-cache-dir dlib==19.24.0

# 7. 나머지 Python 패키지 설치 (requirements.txt 내 모든 패키지 포함)
RUN pip install --no-cache-dir -r requirements.txt

# 8. 애플리케이션 코드 복사
COPY . .

# 9. FastAPI 서버 포트 설정
EXPOSE 8000

# 10. 애플리케이션 실행 (uvicorn 기본 실행)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
