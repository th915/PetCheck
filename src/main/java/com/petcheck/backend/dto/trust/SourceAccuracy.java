package com.petcheck.backend.dto.trust;

/**
 * "출처별 정확도 비교" 한 줄.
 * 공공데이터만 있는 장소와 매장이 등록한 장소를 같은 잣대(리뷰 일치율)로 비교한다.
 *
 * @param storeRegistered true = 매장 등록 장소, false = 공공데이터만 있는 장소
 * @param placeCount      리뷰가 1건 이상인 장소 수
 * @param averageMatchRate 장소별 일치율의 평균 (0.0 ~ 1.0)
 */
public record SourceAccuracy(boolean storeRegistered, long placeCount, Double averageMatchRate) {
}
