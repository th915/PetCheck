# 펫체크 백엔드

2026 관광데이터 활용 공모전 · 웹 구현 부문 · 지정과제 6

Spring Boot 3.5 · Java 21 · Spring Data JPA + QueryDSL · MySQL 8

---

## 1. 실행 전 준비

### JDK 21

```bash
brew install --cask temurin@21
java -version   # 21.x 확인
```

IntelliJ에서 받아도 된다. `File → Project Structure → SDK → Download JDK → Temurin 21`

### MySQL

```bash
brew install mysql
brew services start mysql
mysql -u root < db/init.sql
```

`db/init.sql` 안의 `원하는비밀번호`를 먼저 실제 비밀번호로 바꾸고 실행한다.

### .env

```bash
cp .env.example .env
```

`.env`를 열어 `DB_PASSWORD`를 위에서 정한 비밀번호로 채운다.
API 키는 발급받는 대로 하나씩 추가하면 된다 (비어 있어도 기동은 된다).

---

## 2. 실행

```bash
./gradlew bootRun
```

IntelliJ에서는 `BackendApplication` 의 ▶ 버튼.

```bash
curl localhost:8080/api/health
# {"status":"ok","time":"..."}
```

또는 `http/health.http` 파일을 열고 ▶ 버튼 (Postman 필요 없음).

### 테스트

```bash
./gradlew test              # DB 없이 도는 것만 (도메인 로직, 컨트롤러 슬라이스)
./gradlew integrationTest   # 로컬 MySQL 필요 (QueryDSL 검색 검증)
```

`integration` 태그가 붙은 테스트는 기본 `test` 에서 제외된다.
MySQL이 꺼져 있다고 빌드가 깨지지 않게 하려는 것이다.

---

## 3. 패키지 구조

레이어를 먼저 나누고, 그 아래를 도메인으로 나눈다.

```
com/petcheck/backend/
├── BackendApplication.java
│
├── config/                        스프링 설정
│   ├── JpaAuditingConfig          생성·수정 시각 자동 기록
│   ├── QuerydslConfig             JPAQueryFactory 빈
│   ├── SecurityConfig             로컬 permitAll + BCrypt
│   ├── StaticResourceConfig       /uploads/** 서빙
│   └── WebConfig                  CORS
│
├── common/                        도메인에 속하지 않는 공통
│   ├── properties/TrustScoreProperties
│   └── storage/LocalImageStorage
│
├── entity/                        JPA 엔티티
│   ├── common/BaseTimeEntity
│   ├── member/    Member, MemberRole
│   ├── pet/       PetProfile, PetSize, PetTrait
│   ├── place/     Place, PetPolicy, PlaceCategory, SourceType, TrustGrade
│   ├── store/     Store, LegalChecklist, VerificationStatus
│   ├── review/    Review, MismatchReason
│   ├── trust/     TrustScoreHistory
│   ├── favorite/  Favorite
│   └── schedule/  VisitSchedule
│
├── repository/                    데이터 접근
│   ├── member/    MemberRepository
│   ├── pet/       PetProfileRepository
│   ├── place/     PlaceRepository
│   │              PlaceQueryRepository       (동적 검색 인터페이스)
│   │              PlaceQueryRepositoryImpl   (QueryDSL 구현)
│   ├── store/     StoreRepository
│   ├── review/    ReviewRepository
│   ├── trust/     TrustScoreHistoryRepository
│   │              TrustStatsRepository       (통계 인터페이스)
│   │              TrustStatsRepositoryImpl   (QueryDSL 구현)
│   ├── favorite/  FavoriteRepository
│   └── schedule/  VisitScheduleRepository
│
├── dto/                           계층 간 전달 객체
│   ├── place/     PlaceSearchCondition, PlaceSortType, PlaceSummary
│   ├── review/    MismatchReasonCount
│   └── trust/     AccuracyDashboard, SourceAccuracy, TrustTrendPoint
│
└── controller/                    HTTP 엔드포인트
    └── health/HealthController
```

`service/` 는 아직 비어 있다. 다음 단계에서 같은 규칙으로 도메인별 하위 폴더가 생긴다.

### 커스텀 리포지토리 규약

Spring Data 는 이름으로 구현체를 찾는다. 이름이 어긋나면 조용히 빈 주입만 실패한다.

```
PlaceQueryRepository        ← 인터페이스
PlaceQueryRepositoryImpl    ← 반드시 "인터페이스명 + Impl"
PlaceRepository extends JpaRepository<Place, Long>, PlaceQueryRepository
```

`TrustStatsRepository` / `TrustStatsRepositoryImpl` 도 같은 규칙이고,
`TrustScoreHistoryRepository` 가 둘을 합친다.

---

## 4. 도메인 모델

```
Member ──┬── PetProfile      (1마리 = 1행, 행 개수 = 마릿수)
         ├── Review          ──┐
         ├── Favorite        ──┤
         ├── VisitSchedule   ──┤
         └── Store (owner)   ──┤
                               ▼
                            Place ── TrustScoreHistory (스냅샷, append only)
                              │
                              └── PetPolicy (@Embedded)
```

| 엔티티 | 역할 |
|---|---|
| `Place` | 장소 하나. 공공데이터 4종이 전부 이 모양으로 정규화된다. 신뢰도 점수를 직접 보유 |
| `Store` | 매장이 등록한 레이어. `Place` 하나당 최대 하나 |
| `PetPolicy` | 동반 조건. `@Embeddable` 이라 `place` 테이블에 컬럼으로 펼쳐진다 |
| `LegalChecklist` | 식품위생법 시행규칙 8개 항목. `store.legal_checklist` JSON 컬럼 |
| `Review` | 별점이 아니라 `matched` 하나가 핵심. 공식의 `match_i` |
| `TrustScoreHistory` | 재계산 시점 스냅샷. 정확도 대시보드 추이 차트의 원본 |
| `PetProfile` | 견종·체중·특성 |
| `Favorite` | 관심 장소. 등급 변경 알림의 수신 대상 |
| `VisitSchedule` | 캘린더 일정 |

### 신뢰도 공식

`Place.recalculateTrustScore(double weight)` 한 곳에만 있다.

```
trust_score = (C × m + Σ match_i) / (C + n)
```

- `m` = `Place.priorScore()` — 법정기준 인증 0.8 / 지자체 등록 확인 0.65 / 공공데이터만 0.5
- `C` = `app.trust.weight` (기본 5)
- `n`, `Σ match_i` = `reviewCount`, `matchedReviewCount` 누적 컬럼

리뷰를 저장할 때 집계 쿼리를 돌리지 않는다. `Review` 생성 시
`Place.accumulateReview()` 로 카운터만 올리고, 점수 재계산은 배치가 한다.
`recalculateTrustScore()` 는 **재계산 이전 등급**을 반환하므로
배치가 그 값과 비교해 등급이 바뀐 장소만 골라 알림을 보낸다.

---

## 5. 저장 형태를 고른 기준

**검색 조건이 되는 값은 JSON 컬럼에 넣지 않는다.** 이 한 문장이 세 군데를 갈랐다.

| 데이터 | 저장 형태 | 이유 |
|---|---|---|
| `PetPolicy` 크기 허용 여부 | `allow_small` / `allow_medium` / `allow_large` **boolean 컬럼** | 검색 필터다. JSON이면 `JSON_CONTAINS` 를 불러야 하고 인덱스를 못 탄다 |
| `Store` 전 항목 충족 여부 | `legal_certified` **boolean 컬럼** | 체크리스트가 JSON이라 SQL에서 8개 항목을 뒤질 수 없다. 승인·수정 경로에서 `syncCertification()` 이 함께 갱신한다 |
| `Review` 불일치 사유 | `review_mismatch_reason` **@ElementCollection 테이블** | "어떤 정보가 자주 틀리는가"를 `GROUP BY` 해야 한다 |
| `LegalChecklist` 8개 항목 | **JSON 컬럼** | 상세 화면 표시용이고 검색에 안 쓴다. 컬럼 8개를 늘릴 이유가 없다 |
| `PetProfile.traits` | **JSON 컬럼** | 화면에 뿌리기만 한다 |

`Boolean` 래퍼를 쓴 자리는 전부 의도적이다. `null` = 정보 없음, `false` = 불가.
공공데이터에는 빈 필드가 흔한데 둘을 합치면 "정보가 없어서 안 나온 것"과
"안 된다고 해서 안 나온 것"을 구분할 수 없다.

---

## 6. 동적 검색 (`PlaceQueryRepositoryImpl`)

조건 하나가 메서드 하나다. 조건이 없으면 `null` 을 반환하고,
QueryDSL 의 `where()` 가 `null` 인자를 건너뛴다. `if` 로 쿼리를 조립할 일이 없다.

```java
private BooleanExpression[] predicates(PlaceSearchCondition c) {
    return new BooleanExpression[]{
            keywordContains(c.keyword()),
            categoryIn(c.categories()),
            careFacilityOnly(c.careFacilityOnly()),
            admitsPet(c.petSize(), c.petWeightKg()),
            indoorRequired(c.indoorRequired()),
            legalCertifiedOnly(c.legalCertifiedOnly()),
            minTrustScore(c.minTrustScore()),
            withinBoundingBox(c)
    };
}
```

### 반경 검색을 정사각형으로 근사한 이유

원형 반경을 정확히 구하려면 `ST_Distance_Sphere` 같은 DB 함수가 필요한데,
함수 결과에는 인덱스가 걸리지 않아 장소 전체를 훑게 된다.
`(latitude, longitude)` 인덱스를 태우려면 범위 비교여야 한다.
모서리 쪽이 최대 41% 넓게 잡히지만, 정렬이 거리순이고 상위 N개만 나가므로 체감되지 않는다.

### 거리 정렬에 DB 함수를 쓰지 않은 이유

```java
double cosLat = Math.cos(Math.toRadians(c.latitude()));

NumberExpression<Double> dLat = place.latitude.subtract(c.latitude());
NumberExpression<Double> dLng = place.longitude.subtract(c.longitude()).multiply(cosLat);

return dLat.multiply(dLat).add(dLng.multiply(dLng));
```

정렬만 하면 되므로 제곱근을 씌우지 않는다.
경도 1도의 실제 거리는 위도에 따라 줄어드는데, `cos(중심위도)` 를 자바에서
상수로 계산해 곱하는 것으로 보정이 끝난다. MySQL 방언에 묶이지 않는다.
화면에 찍을 km 는 `PlaceSummary.distanceKmFrom()` 이 만든다.

### 배치의 N+1

`Place.priorScore()` 가 `store` 를 읽는데 `@OneToOne(mappedBy)` 는 `null` 일 수 있어
Hibernate 가 프록시를 만들지 못하고 장소마다 쿼리를 낸다.
`PlaceRepository.findBatchForRecalculation()` 이 `join fetch` 로 막는다.
offset 대신 id 커서를 쓰는 것도 같은 이유 — 배치가 뒤로 갈수록 offset 이 느려진다.

---

## 7. 확인이 필요한 한 곳

`TrustStatsRepositoryImpl.trendSince()` 의 날짜 그룹핑이다.

```java
StringTemplate day = Expressions.stringTemplate(
        "format({0} as 'yyyy-MM-dd')", trustScoreHistory.calculatedAt);
```

`format(x as '패턴')` 은 Hibernate 6 가 자체 제공하는 HQL 함수라
`DATE_FORMAT` 같은 MySQL 전용 함수를 부르지 않는다.
동작하지 않으면 `function('DATE_FORMAT', {0}, '%Y-%m-%d')` 로 바꾸면 된다.

나머지는 `./gradlew integrationTest` 가 실제 MySQL 위에서 검증한다.

---

## 8. 프론트엔드 연결

`frontend/next.config.ts`

```ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/backend/:path*",
        destination: `${process.env.BACKEND_URL}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
```

`frontend/.env.local`

```
BACKEND_URL=http://localhost:8080
```

`source` 를 `/api/:path*` 가 아니라 `/api/backend/:path*` 로 둔 이유:
App Router 의 Route Handler(`src/app/api/...`)와 경로가 겹치면 rewrite 가 무시된다.

```bash
cd backend && ./gradlew bootRun     # 8080
cd frontend && npm run dev          # 3000
```

---

## 9. 다음 작업

1. `service/` — 매장 등록·승인, 리뷰 작성, 검색 유스케이스
2. 공공데이터 4종 응답 → `Place` / `PetPolicy` 매핑 계층
3. `@Scheduled` 신뢰도 재계산 배치 + `TrustScoreHistory` 적재 + 등급 변경 감지
4. SSE 알림 엔드포인트
5. `controller/` — 검색·매장·리뷰·대시보드 API

`@EnableScheduling` 과 `@ConfigurationPropertiesScan` 은 `BackendApplication` 에 이미 붙어 있다.

---

## 10. 버전 참고

`build.gradle` 의 Spring Boot 버전이 `3.5.5` 로 고정돼 있다.
IntelliJ 가 빨간 줄을 그으면 최신 3.x 패치로 올려도 된다.
QueryDSL 은 `5.1.0:jakarta` — **`:jakarta` 분류자를 빼면 컴파일이 깨진다.**
Spring Boot 3 이 `javax` → `jakarta` 로 넘어갔기 때문이다.
