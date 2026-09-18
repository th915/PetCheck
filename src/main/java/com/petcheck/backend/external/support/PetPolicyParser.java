package com.petcheck.backend.external.support;

import com.petcheck.backend.entity.pet.PetSize;
import com.petcheck.backend.entity.place.PetPolicy;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 공공데이터의 자유 서술 문자열을 PetPolicy 로 옮긴다.
 * <p>
 * 실제 데이터를 보고 맞춘 버전이다. 문화정보원 7만 행의 실제 값 분포:
 * "모두 가능" 94%, "해당없음" 4%, 나머지가 "소형/중형", "5kg 미만 소형" 같은 자유 서술.
 * <p>
 * null 과 false 를 끝까지 구분한다.
 * "해당없음"은 정보가 없다는 뜻이지 불가하다는 뜻이 아니다.
 */
public final class PetPolicyParser {

    /** 정보 없음을 뜻하는 값. 여기 걸리면 false 가 아니라 null 이 된다 */
    private static final Set<String> NO_DATA = Set.of("해당없음", "해당 없음", "-", "");

    /** "10,000원", "3마리부터 견당 3,000원" 에서 금액만 뽑는다 */
    private static final Pattern FEE_WON = Pattern.compile("(\\d[\\d,]*)\\s*원");

    private PetPolicyParser() {
    }

    /**
     * @param sizeText        입장 가능 동물 크기
     * @param restrictionText 반려동물 제한사항
     * @param indoorText      장소(실내) 여부  — Y / N
     * @param outdoorText     장소(실외)여부   — Y / N
     * @param feeText         애견 동반 추가 요금
     */
    /**
     * @param allowedText 반려동물 동반 가능정보 — Y / N. 다른 모든 조건의 상위 게이트
     */
    public static PetPolicy parse(String allowedText, String sizeText, String restrictionText,
                                  String indoorText, String outdoorText, String feeText) {

        String size = normalize(sizeText);
        String restriction = normalize(restrictionText);
        String conditions = merge(size, restriction);

        SizeAllowance allowance = SizeAllowance.of(size);

        return PetPolicy.builder()
                .petAllowed(yesNo(allowedText))
                .allowSmall(allowance.small())
                .allowSmall(allowance.small())
                .allowMedium(allowance.medium())
                .allowLarge(allowance.large())
                .weightLimitKg(allowance.weightLimitKg())
                .indoorAllowed(yesNo(indoorText))
                .outdoorAllowed(yesNo(outdoorText))
                .carrierRequired(requires(conditions, "이동장", "케이지", "캔넬", "캐리어", "유모차", "백팩"))
                .leashRequired(requires(conditions, "목줄", "리드줄", "하네스"))
                .vaccinationRequired(requires(conditions, "접종", "예방주사", "광견병"))
                .extraFee(fee(feeText))
                .restrictionNote(restrictionNote(restriction))
                .build();
    }

    /**
     * 크기 허용 범위. 크기 제한은 "상한"을 말하므로
     * "중형"이라고 적혀 있으면 소형도 당연히 가능한 것으로 읽는다.
     */
    private record SizeAllowance(Boolean small, Boolean medium, Boolean large, Double weightLimitKg) {

        private static final SizeAllowance UNKNOWN = new SizeAllowance(null, null, null, null);
        private static final SizeAllowance ALL = new SizeAllowance(true, true, true, null);

        private static final Pattern WEIGHT_KG =
                Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:kg|킬로)", Pattern.CASE_INSENSITIVE);

        static SizeAllowance of(String text) {
            if (text == null) {
                return UNKNOWN;
            }

            // kg 표기가 가장 구체적이므로 먼저 본다. "5kg 미만 소형" 같은 복합 표기 때문이다.
            Matcher matcher = WEIGHT_KG.matcher(text);
            if (matcher.find()) {
                double limit = Double.parseDouble(matcher.group(1));
                PetSize max = PetSize.fromWeight(limit);
                return new SizeAllowance(true, max != PetSize.SMALL, max == PetSize.LARGE, limit);
            }

            if (containsAny(text, "모두", "모든", "제한없", "제한 없", "전체", "전견")) {
                return ALL;
            }
            if (text.contains("대형")) {
                return ALL;
            }
            if (text.contains("중형")) {
                return new SizeAllowance(true, true, false, null);
            }
            if (text.contains("소형")) {
                return new SizeAllowance(true, false, false, null);
            }

            // "고양이", "포유류 특수동물, 조류, 파충류" — 개 크기 정보가 아니다
            return UNKNOWN;
        }
    }

    /** 요구 조건. 원문 자체가 없으면 false 가 아니라 null 이다 */
    private static Boolean requires(String conditions, String... keywords) {
        if (conditions == null) {
            return null;
        }
        return containsAny(conditions, keywords);
    }

    /** "없음"은 무료(0원)라는 뜻이다. "변동"처럼 금액이 없으면 null */
    private static Integer fee(String feeText) {
        String text = normalize(feeText);
        if (text == null) {
            return null;
        }
        if (containsAny(text, "없음", "무료")) {
            return 0;
        }
        Matcher matcher = FEE_WON.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1).replace(",", "")) : null;
    }

    /** "제한사항 없음"은 화면에 띄울 내용이 아니다. 저장하지 않는다 */
    private static String restrictionNote(String restriction) {
        if (restriction == null || containsAny(restriction, "제한사항 없음", "제한 사항 없음")) {
            return null;
        }
        return restriction;
    }

    private static Boolean yesNo(String text) {
        String value = normalize(text);
        if (value == null) {
            return null;
        }
        if (containsAny(value, "Y", "가능", "O", "있음")) {
            return true;
        }
        if (containsAny(value, "N", "불가", "X", "없음")) {
            return false;
        }
        return null;
    }

    /** 크기와 제한사항을 합친다. 목줄·이동장 요구가 양쪽 어디에나 적힐 수 있다 */
    private static String merge(String size, String restriction) {
        if (size == null && restriction == null) {
            return null;
        }
        return (size == null ? "" : size) + " " + (restriction == null ? "" : restriction);
    }

    private static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return NO_DATA.contains(trimmed) ? null : trimmed;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}