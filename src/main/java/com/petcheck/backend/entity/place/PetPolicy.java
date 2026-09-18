package com.petcheck.backend.entity.place;

import com.petcheck.backend.entity.pet.PetSize;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공공데이터 4종의 동반 조건 필드를 하나로 정규화한 결과.
 * 기관마다 필드명과 단위가 달라서(제안서 "학습·고민 포인트" 3번),
 * 원본 문자열을 두지 않고 전부 이 모양으로 옮긴 뒤 저장한다.
 * <p>
 * <b>크기 허용 여부를 Set&lt;PetSize&gt; JSON 이 아니라 boolean 세 칸으로 둔 이유:</b>
 * 이 값이 검색 조건이기 때문이다. JSON 컬럼에 넣으면 MySQL 에서
 * JSON_CONTAINS 를 호출해야 하고 인덱스를 못 탄다.
 * boolean 컬럼이면 QueryDSL 에서 평범한 비교 한 줄로 끝난다.
 * <p>
 * Embeddable 이므로 place 테이블에 컬럼으로 펼쳐진다. 조인이 생기지 않는다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetPolicy {

    /**
     * 동반 가능 여부 자체. 다른 모든 조건의 상위에 있다.
     * null = 정보 없음, false = 동반 불가.
     * 문화정보원 데이터에서 이 값이 N 인 행은 크기·제한사항이 전부 "해당없음"이다.
     */
    @Column(name = "pet_allowed")
    private Boolean petAllowed;

    /** null = 정보 없음, false = 불가. 둘을 구분해야 하므로 primitive 를 쓰지 않는다. */
    @Column(name = "allow_small")
    private Boolean allowSmall;

    @Column(name = "allow_medium")
    private Boolean allowMedium;

    @Column(name = "allow_large")
    private Boolean allowLarge;

    /** 매장이 직접 kg 상한을 건 경우. null 이면 크기 허용 여부를 따른다. */
    @Column(name = "weight_limit_kg")
    private Double weightLimitKg;

    @Column(name = "indoor_allowed")
    private Boolean indoorAllowed;

    @Column(name = "outdoor_allowed")
    private Boolean outdoorAllowed;

    @Column(name = "carrier_required")
    private Boolean carrierRequired;

    @Column(name = "leash_required")
    private Boolean leashRequired;

    @Column(name = "vaccination_required")
    private Boolean vaccinationRequired;

    /** 동반 추가 요금(원). 0 이면 무료, null 이면 정보 없음. */
    @Column(name = "extra_fee")
    private Integer extraFee;

    @Column(name = "restriction_note", length = 500)
    private String restrictionNote;

    @Builder
    public PetPolicy(Boolean petAllowed,
                     Boolean allowSmall, Boolean allowMedium, Boolean allowLarge,
                     Double weightLimitKg, Boolean indoorAllowed, Boolean outdoorAllowed,
                     Boolean carrierRequired, Boolean leashRequired, Boolean vaccinationRequired,
                     Integer extraFee, String restrictionNote) {
        this.petAllowed = petAllowed;
        this.allowSmall = allowSmall;
        this.allowMedium = allowMedium;
        this.allowLarge = allowLarge;
        this.weightLimitKg = weightLimitKg;
        this.indoorAllowed = indoorAllowed;
        this.outdoorAllowed = outdoorAllowed;
        this.carrierRequired = carrierRequired;
        this.leashRequired = leashRequired;
        this.vaccinationRequired = vaccinationRequired;
        this.extraFee = extraFee;
        this.restrictionNote = restrictionNote;
    }

    /**
     * 화면 안내 문구용. 검색 필터는 이 메서드가 아니라
     * PlaceQueryRepositoryImpl 의 admitsPet() 이 같은 규칙을 SQL 로 옮겨 쓴다.
     *
     * @return 판단할 정보가 없으면 null
     */
    public Boolean admits(PetSize size, Double weightKg) {
        if (weightLimitKg != null && weightKg != null) {
            return weightKg <= weightLimitKg;
        }
        return switch (size) {
            case SMALL -> allowSmall;
            case MEDIUM -> allowMedium;
            case LARGE -> allowLarge;
        };
    }
}
