# PetCheck

반려동물과의 여행을 위한 장소 검색 + 일정(트립) 관리 웹사이트.

- Java 17, Spring Boot 4.1.1, Gradle
- MySQL 8
- Thymeleaf + Vanilla JS
- Kakao Maps JS SDK
- Spring Security (세션 기반 로그인)

## 1. 사전 준비

### 1-1. MySQL 스키마 생성

MySQL에 접속해서 아래 두 스크립트를 **순서대로** 실행하세요.

```bash
mysql -u root -p < petcheck_init.sql
mysql -u root -p < db/petcheck_trip_tables.sql
mysql -u root -p < db/petcheck_kakao_source.sql
```

- `petcheck_init.sql`: 제공된 원본 스키마 + 예시 데이터 (10개 테이블)
- `db/petcheck_trip_tables.sql`: 여행 일정(트립) 기능을 위해 추가된 `trip`, `trip_place` 2개 테이블
- `db/petcheck_kakao_source.sql`: 카카오맵 검색 결과를 저장할 수 있도록 `place.source_type` ENUM에 `KAKAO_LOCAL` 값 추가

> 원본 스키마에는 "여행 일정"을 표현할 테이블이 없어 (기존 `visit_schedule`은 리뷰 신뢰도 인증용 단건 방문예약이라 여행이름/기간/일차별 순서를 담지 못함) 두 테이블을 추가로 정의했습니다. 기존 10개 테이블은 손대지 않았습니다.

### 1-2. 카카오맵 키 발급 (2개 모두 필요)

[Kakao Developers](https://developers.kakao.com/) 에서 애플리케이션을 등록하면 "앱 키" 탭에 여러 키가 발급됩니다. 이 중 2개를 사용합니다.

- **JavaScript 키** (`KAKAO_MAP_JS_KEY`): 지도 렌더링용. 플랫폼 설정에 실행 도메인(`http://localhost:8080` 등)을 Web 플랫폼으로 등록해야 합니다.
- **REST API 키** (`KAKAO_REST_API_KEY`): 장소 키워드 검색(카카오맵 전체 장소 검색)을 서버에서 호출하는 데 사용합니다. 도메인 등록이 필요 없습니다.

둘 다 없어도 앱은 실행되며, 지도 영역엔 안내 메시지가 표시되고 검색은 DB에 등록된 장소만으로 동작합니다. REST 키만 없으면 지도는 뜨지만 검색 결과에 카카오맵 장소는 섞이지 않고 DB 장소만 나옵니다.

### 1-3. 카카오맵 장소 검색 동작 방식

키워드로 검색하면 DB에 등록된 장소 + 카카오맵 키워드 검색 결과(`/v2/local/search/keyword.json`, 반경 20km)를 합쳐서 보여줍니다. 카카오맵에서만 존재하던 장소를 처음 검색하면 그 시점에 `place` 테이블에 최소 정보(이름/주소/좌표/카테고리, 신뢰도는 기본값 UNCERTAIN)로 자동 등록되어 이후 즐겨찾기·일정 등록·리뷰 등 앱의 모든 기능을 동일하게 사용할 수 있습니다. 목록/상세에서 "카카오맵" 태그로 구분 표시됩니다.

## 2. 환경변수

| 변수명 | 설명 | 기본값 |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/petcheck?...` |
| `DB_USERNAME` | DB 사용자 | `root` |
| `DB_PASSWORD` | DB 비밀번호 | (없음) |
| `KAKAO_MAP_JS_KEY` | 카카오맵 JavaScript 키 | (없음) |
| `KAKAO_REST_API_KEY` | 카카오 REST API 키 (장소 검색용) | (없음) |
| `SERVER_PORT` | 서버 포트 | `8080` |

PowerShell 예시:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
$env:KAKAO_MAP_JS_KEY="your-kakao-js-key"
$env:KAKAO_REST_API_KEY="your-kakao-rest-key"
./gradlew.bat bootRun
```

## 3. 실행

```bash
./gradlew bootRun
```

브라우저에서 `http://localhost:8080` 접속.

## 4. 참고

- `spring.jpa.hibernate.ddl-auto=none` 입니다. 스키마는 위 SQL 스크립트로만 관리합니다.
- 시드 데이터의 회원 비밀번호는 해시값만 있어 원문을 알 수 없으므로, 로그인 테스트는 `/signup`으로 새 계정을 만들어 진행하세요.
- 리뷰 작성, 매장(사장님) 대시보드, 관리자 기능은 이번 범위에 포함되지 않았습니다 (조회만 지원).

## 5. 빌드 환경 관련 안내

이 코드는 Java/Gradle이 설치되지 않은 환경에서 작성되어 로컬에서 직접 컴파일 검증을 하지 못했습니다. `./gradlew build` 실행 후 컴파일 에러가 발생하면 (특히 Spring Boot 4.x API 변경으로 인한 것) 알려주시면 바로 수정하겠습니다.
