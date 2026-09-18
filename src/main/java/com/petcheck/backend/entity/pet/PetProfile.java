package com.petcheck.backend.entity.pet;

import com.petcheck.backend.entity.common.BaseTimeEntity;
import com.petcheck.backend.entity.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 반려동물 한 마리당 한 행. 행 개수가 곧 "마릿수"다.
 * <p>
 * traits 는 화면에 뿌리기만 하고 검색 조건으로 쓰지 않으므로 JSON 컬럼으로 둔다.
 * (검색 조건으로 쓰는 값은 JSON 에 넣으면 안 된다 — PetPolicy 주석 참고)
 */
@Getter
@Entity
@Table(
        name = "pet_profile",
        indexes = @Index(name = "idx_pet_profile_member", columnList = "member_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 50)
    private String breed;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetSize size;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Set<PetTrait> traits = new LinkedHashSet<>();

    @Column(length = 300)
    private String note;

    @Builder
    public PetProfile(Member member, String name, String breed, Double weightKg,
                      Set<PetTrait> traits, String note) {
        this.member = member;
        this.name = name;
        this.breed = breed;
        this.weightKg = weightKg;
        this.size = PetSize.fromWeight(weightKg);
        this.traits = traits == null ? new LinkedHashSet<>() : new LinkedHashSet<>(traits);
        this.note = note;
    }

    public void update(String name, String breed, Double weightKg, Set<PetTrait> traits, String note) {
        this.name = name;
        this.breed = breed;
        this.weightKg = weightKg;
        this.size = PetSize.fromWeight(weightKg);
        this.traits = traits == null ? new LinkedHashSet<>() : new LinkedHashSet<>(traits);
        this.note = note;
    }
}
