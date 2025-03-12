import os
import cv2
import torch
import numpy as np
from PIL import Image
from fastapi import FastAPI, File, UploadFile, HTTPException, Form, APIRouter, Body
from fastapi.middleware.cors import CORSMiddleware
from torchvision import transforms
import clip
import io
import uvicorn
import json
import requests
from typing import List, Dict, Any, Optional
from pydantic import BaseModel
from detector import detector
import detector

app = FastAPI()
router = APIRouter()

# CORS 설정 추가
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

THRESHOLD = 0.85  # 유사도 임계값 (0.85 이상이면 연관된 동물로 판정)
class ImageRequest(BaseModel):
    image_url: str

class CompareEmbeddingsRequest(BaseModel):
    finding_embedding: list
    sighted_embedding: list

# ================ #
# DB에서 이미지 정보 가져오기 #
# ================ #
def get_existing_images_by_status(status: str):
    """DB에서 특정 상태(FINDING, SIGHTED)의 이미지 리스트 가져오기"""
    response = requests.get(f"http://43.201.55.18:8090/api/images?status={status}")  # Java 서버 API 호출
    if response.status_code != 200:
        return []
    return response.json().get("images", [])

# ================ #
# URL에서 임베딩 추출 #
# ================ #
def extract_embedding_from_url(url: str):
    """URL에서 이미지를 불러와 임베딩 및 특징 추출"""
    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()

        # ✅ 이미지 정상 다운로드 확인
        if response.status_code != 200:
            return {"embedding": [], "features": [], "success": False, "error": "이미지 요청 실패"}

        # 이미지 바이트를 버퍼로 변환
        image_bytes = io.BytesIO(response.content)

        # PIL 이미지로 로드
        image_pil = Image.open(image_bytes)

        # 이미지 모드 확인 및 변환
        if image_pil.mode != 'RGB':
            print(f"이미지 모드 변환: {image_pil.mode} -> RGB")
            image_pil = image_pil.convert('RGB')

        # RGB 이미지를 numpy 배열로 변환
        image_cv = np.array(image_pil)

        print(f"이미지 로드 성공: {url}, shape={image_cv.shape}, dtype={image_cv.dtype}")

        # 이미지가 8비트가 아니면 변환
        if image_cv.dtype != np.uint8:
            print(f"이미지 데이터 타입 변환: {image_cv.dtype} -> uint8")
            if image_cv.dtype == np.uint16:
                image_cv = (image_cv / 256).astype(np.uint8)
            else:
                image_cv = image_cv.astype(np.uint8)

        # 차원 확인 및 변환 (2D -> 3D)
        if image_cv.ndim == 2:
            print("그레이스케일 이미지를 RGB로 변환")
            image_cv = cv2.cvtColor(image_cv, cv2.COLOR_GRAY2RGB)

        # 채널 확인 및 변환 (RGBA -> RGB)
        elif image_cv.shape[2] == 4:
            print("RGBA 이미지를 RGB로 변환")
            image_cv = cv2.cvtColor(image_cv, cv2.COLOR_RGBA2RGB)

        # 임베딩 추출 전 이미지 형식 최종 확인
        print(f"최종 이미지 형식: shape={image_cv.shape}, dtype={image_cv.dtype}")

        # ✅ 임베딩 추출
        with torch.no_grad():
            try:
                feature, embedding = detector.image_vector(image_cv)
            except Exception as e:
                print(f"❌ detector.image_vector 호출 중 오류: {e}")
                # 이미지 복사본 만들기 (메모리 연속성 보장)
                image_cv_copy = np.ascontiguousarray(image_cv)
                feature, embedding = detector.image_vector(image_cv_copy)

        # ✅ 임베딩이 None이면 오류 반환
        if embedding is None:
            print("❌ 임베딩 생성 실패")
            return {"embedding": [], "features": [], "success": False, "error": "임베딩 생성 실패"}

        return {
            "embedding": embedding.cpu().numpy().flatten().tolist(),
            "features": feature.tolist(),
            "success": True
        }

    except Exception as e:
        print(f"❌ 이미지 로드 또는 임베딩 추출 실패: {e}")
        import traceback
        traceback.print_exc()  # 상세 오류 추적
        return {"embedding": [], "features": [], "success": False, "error": f"임베딩 추출 실패: {str(e)}"}

# ================ #
# FINDING vs SIGHTED 비교 API #
# ================ #
@app.post("/compare_finding_with_sighted")
async def compare_finding_with_sighted(finding_image_url: str = Form(...)):
    """ FINDING 게시글의 이미지와 SIGHTED 게시글 이미지 비교 """

    finding_data = extract_embedding_from_url(finding_image_url)
    if not finding_data["success"]:
        raise HTTPException(status_code=500, detail=f"FINDING 이미지 임베딩 추출 실패: {finding_data.get('error')}")

    finding_embedding = np.array(finding_data["embedding"])
    finding_features = np.array(finding_data["features"])

    # SIGHTED 게시글 목록 가져오기
    sighted_images = get_existing_images_by_status("SIGHTED")
    if not sighted_images:
        return {"error": "SIGHTED 게시글이 존재하지 않음"}

    matched_results = []

    # 유사도 비교
    for sighted in sighted_images:
        sighted_embedding = np.array(sighted["embedding"])
        sighted_features = np.array(sighted["features"])

        similarity = compare_embeddings_and_features(
            finding_embedding, finding_features, sighted_embedding, sighted_features
        )

        if similarity >= THRESHOLD:
            matched_results.append({
                "sighted_post_id": sighted["post_id"],
                "sighted_image": sighted["path"],
                "similarity": similarity
            })

    return {"matched_sighted_posts": matched_results}

# ================ #
# 두 개의 URL 이미지 비교 #
# ================ #
@app.post("/compare-urls")
async def compare_urls(url1: str = Form(...), url2: str = Form(...)):
    """ 두 개의 URL 이미지를 비교 """
    try:
        emb1 = extract_embedding_from_url(url1)
        emb2 = extract_embedding_from_url(url2)

        if not emb1["success"] or not emb2["success"]:
            raise HTTPException(status_code=400, detail="이미지에서 특징을 추출할 수 없습니다.")

        similarity = float(np.dot(np.array(emb1["embedding"]), np.array(emb2["embedding"])))
        similarity = max(-1, min(similarity, 1))

        result = "같은 동물" if similarity >= 0.85 else "다른 동물"
        return {"similarity": similarity, "result": result}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"URL 비교 중 오류 발생: {str(e)}")

# ================ #
# 여러 임베딩과 비교 #
# ================ #
@app.post("/batch-compare-url")
async def batch_compare_url(url: str = Form(...), embeddings_json: str = Form(...)):
    """ URL 이미지와 여러 저장된 임베딩을 비교 """

    embeddings = json.loads(embeddings_json)
    new_data = extract_embedding_from_url(url)
    if not new_data["success"]:
        raise HTTPException(status_code=400, detail="이미지에서 특징을 추출할 수 없습니다.")

    new_embedding = np.array(new_data["embedding"])
    new_features = np.array(new_data["features"])

    results = []
    for animal_id, stored_data in embeddings.items():
        existing_embedding = np.array(stored_data["embedding"])
        existing_features = np.array(stored_data["features"])

        similarity = compare_embeddings_and_features(
            new_embedding, new_features, existing_embedding, existing_features
        )

        results.append({
            "animal_id": animal_id,
            "similarity": similarity,
            "is_match": similarity >= THRESHOLD
        })

    results = sorted(results, key=lambda x: x["similarity"], reverse=True)
    return {"results": results}

# ================ #
# 유사도 비교 함수 #
# ================ #
def compare_embeddings_and_features(embedding1, features1, embedding2, features2):
    """ 두 개의 embedding 및 feature 벡터를 비교하여 유사도를 계산 """

    similarity_scores = []

    if embedding1 is not None and embedding2 is not None:
        emb_sim = torch.nn.functional.cosine_similarity(
            torch.tensor(embedding1), torch.tensor(embedding2), dim=0
        ).item()
        similarity_scores.append(emb_sim)

    if features1 is not None and features2 is not None and len(features1) > 0 and len(features2) > 0:
        norm1, norm2 = np.linalg.norm(features1), np.linalg.norm(features2)
        if norm1 + norm2 > 0:
            feature_sim = 1 - (np.linalg.norm(features1 - features2) / (norm1 + norm2))
            similarity_scores.append(feature_sim)

    return sum(similarity_scores) / len(similarity_scores) if similarity_scores else 0

@app.post("/compare-embeddings")
async def compare_embeddings(data: CompareEmbeddingsRequest):
    """ 두 개의 임베딩을 비교하는 API """
    try:
        finding_embedding = torch.tensor(data.finding_embedding)
        sighted_embedding = torch.tensor(data.sighted_embedding)

        similarity = torch.nn.functional.cosine_similarity(finding_embedding, sighted_embedding, dim=0).item()

        return {"similarity": similarity}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"유사도 비교 중 오류 발생: {str(e)}")

@app.post("/extract-embedding-from-url")
async def extract_embedding_from_url_api(request: ImageRequest):
    """ URL에서 임베딩을 추출하는 API """
    try:
        if not request.image_url:
            raise HTTPException(status_code=400, detail="image_url이 제공되지 않았습니다.")

        print(f"임베딩 추출 요청 URL: {request.image_url}")

        # ✅ FastAPI는 JSON 요청을 받도록 변경
        result = extract_embedding_from_url(request.image_url)

        if not result["success"]:
            error_msg = result.get("error", "임베딩 생성 실패")
            print(f"❌ 임베딩 추출 실패: {error_msg}")
            raise HTTPException(status_code=500, detail=error_msg)

        print(f"✅ 임베딩 추출 성공: 임베딩 길이={len(result['embedding'])}, 특징 길이={len(result['features'])}")
        return result
    except HTTPException as he:
        # 이미 생성된 HTTP 예외는 그대로 전달
        raise he
    except Exception as e:
        print(f"❌ 예상치 못한 오류: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"현재 URL 임베딩 추출 중 오류 발생: {str(e)}")
# ================ #
# FastAPI 실행 #
# ================ #
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
