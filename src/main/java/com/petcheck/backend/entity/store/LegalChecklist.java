package com.petcheck.backend.entity.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 2026년 3월 1일 시행 식품위생법 시행규칙의 반려동물 동반 출입 시설기준.
 * 항목을 Map 이 아니라 필드로 고정한 이유: 항목이 법 조항과 1:1로 대응하고,
 * 근거 조문 링크를 항목별로 붙여야 하므로 키를 임의로 늘릴 수 없다.
 * <p>
 * MySQL JSON 컬럼에 통째로 직렬화된다. 컬럼을 8개 늘리지 않고 한 칸에 넣는다.
 * 검색 조건이 아니라 상세 화면 표시용이므로 JSON 이어도 된다.
 * (검색에 쓰이는 "전 항목 충족 여부"는 Store.legalCertified 컬럼으로 따로 뽑아둔다)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalChecklist {

    /** 반려동물 출입 구역과 일반 구역 분리(칸막이 등) */
    private boolean partitioned;

    /** 반려동물 전용 케이지 또는 이동장 비치 */
    private boolean cageProvided;

    /** 목줄 고정장치 설치 */
    private boolean leashAnchor;

    /** 테이블·의자 덮개 비치 */
    private boolean seatCover;

    /** 반려동물 전용 식기 비치 */
    private boolean petTableware;

    /** 반려동물 전용 쓰레기통 비치 */
    private boolean petWasteBin;

    /** 출입구에 반려동물 동반 가능 표시 */
    private boolean entranceSign;

    /** 예방접종 확인 절차 운영 */
    private boolean vaccinationCheck;

    @Builder
    public LegalChecklist(boolean partitioned, boolean cageProvided, boolean leashAnchor,
                          boolean seatCover, boolean petTableware, boolean petWasteBin,
                          boolean entranceSign, boolean vaccinationCheck) {
        this.partitioned = partitioned;
        this.cageProvided = cageProvided;
        this.leashAnchor = leashAnchor;
        this.seatCover = seatCover;
        this.petTableware = petTableware;
        this.petWasteBin = petWasteBin;
        this.entranceSign = entranceSign;
        this.vaccinationCheck = vaccinationCheck;
    }

    /**
     * 전 항목 충족 시에만 "법정 기준 인증" 배지가 붙는다.
     * <p>
     * &#64;JsonIgnore 가 없으면 이 클래스를 JSON 컬럼으로 직렬화할 때
     * "fullySatisfied" 라는 가짜 필드가 같이 저장되고, 다시 읽을 때 깨진다.
     * satisfiedCount() 는 get/is 로 시작하지 않아 getter 로 인식되지 않는다.
     */
    @JsonIgnore
    public boolean isFullySatisfied() {
        return partitioned && cageProvided && leashAnchor && seatCover
                && petTableware && petWasteBin && entranceSign && vaccinationCheck;
    }

    public int satisfiedCount() {
        int count = 0;
        for (boolean item : new boolean[]{partitioned, cageProvided, leashAnchor, seatCover,
                petTableware, petWasteBin, entranceSign, vaccinationCheck}) {
            if (item) {
                count++;
            }
        }
        return count;
    }
}
