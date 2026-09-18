package com.petcheck.backend.entity.favorite;

import com.petcheck.backend.entity.common.BaseTimeEntity;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.place.Place;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관심 장소 등록. 웹 알림의 수신 대상이 여기서 나온다.
 * 배치가 등급이 바뀐 Place 를 찾으면, 그 Place 를 관심 등록한 회원에게 SSE 이벤트를 보낸다.
 */
@Getter
@Entity
@Table(
        name = "favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_member_place",
                columnNames = {"member_id", "place_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Builder
    public Favorite(Member member, Place place) {
        this.member = member;
        this.place = place;
    }
}
