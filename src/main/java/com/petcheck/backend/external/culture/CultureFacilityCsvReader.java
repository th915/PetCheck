package com.petcheck.backend.external.culture;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.common.properties.ExternalApiProperties;
import com.petcheck.backend.entity.place.SourceType;
import com.petcheck.backend.external.dto.PlaceSyncData;
import com.petcheck.backend.external.support.CategoryMapper;
import com.petcheck.backend.external.support.PetPolicyParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한국문화정보원 "전국 반려동물 동반 가능 문화시설 위치 데이터" CSV 를 읽는다.
 * <p>
 * 오픈API 가 아니라 파일 데이터셋이므로 공공데이터포털에서 내려받아
 * external.culture-data.csv-path 에 둔 파일을 읽는다.
 * <p>
 * 실패하면 빈 리스트가 아니라 예외를 던진다. 조용히 0건을 돌려주면
 * "동기화는 됐는데 데이터가 없다"를 한참 뒤에야 알아차리게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CultureFacilityCsvReader {

    private final ExternalApiProperties properties;

    // 컬럼 순서가 아니라 헤더 이름으로 찾는다. 데이터셋이 갱신돼도 안 깨진다.
    private static final String COL_NAME = "시설명";
    private static final String COL_CATEGORY1 = "카테고리1";
    private static final String COL_CATEGORY2 = "카테고리2";
    private static final String COL_CATEGORY3 = "카테고리3";
    private static final String COL_ROAD_ADDRESS = "도로명주소";
    private static final String COL_LOT_ADDRESS = "지번주소";
    private static final String COL_PHONE = "전화번호";
    private static final String COL_LATITUDE = "위도";
    private static final String COL_LONGITUDE = "경도";
    private static final String COL_PET_ALLOWED = "반려동물 동반 가능정보";
    private static final String COL_PET_SIZE = "입장 가능 동물 크기";
    private static final String COL_RESTRICTION = "반려동물 제한사항";
    private static final String COL_INDOOR = "장소(실내) 여부";
    private static final String COL_OUTDOOR = "장소(실외)여부";
    private static final String COL_EXTRA_FEE = "애견 동반 추가 요금";
    // 도로명·지번 주소가 모두 비었을 때 조합해 쓰는 행정구역 조각
    private static final String COL_SIDO = "시도 명칭";
    private static final String COL_SIGUNGU = "시군구 명칭";
    private static final String COL_EUPMYEONDONG = "법정읍면동명칭";
    private static final String COL_RI = "리 명칭";
    private static final String COL_BUNJI = "번지";

    /** 이 셋이 없으면 장소를 만들 수 없다. 나머지는 비어도 진행한다. */
    private static final String[] REQUIRED_COLUMNS = {COL_NAME, COL_LATITUDE, COL_LONGITUDE};

    public List<PlaceSyncData> read() {
        Path path = Paths.get(properties.cultureData().csvPath()).toAbsolutePath();

        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.SYNC_SOURCE_UNAVAILABLE,
                    "CSV 파일이 없습니다: " + path
                            + " — 공공데이터포털에서 내려받아 이 경로에 두세요");
        }

        Charset charset = Charset.forName(properties.cultureData().charset());
        List<PlaceSyncData> result = new ArrayList<>();
        int skipped = 0;

        try (CSVReader reader = new CSVReader(
                new BufferedReader(new InputStreamReader(Files.newInputStream(path), charset)))) {

            Map<String, Integer> header = readHeader(reader, charset);
            String[] row;

            while ((row = reader.readNext()) != null) {
                PlaceSyncData data = toSyncData(row, header);
                if (data != null && data.isUsable()) {
                    result.add(data);
                } else {
                    skipped++;
                }
            }

        } catch (IOException | CsvValidationException e) {
            throw new BusinessException(ErrorCode.SYNC_SOURCE_UNAVAILABLE,
                    "CSV 를 읽던 중 실패했습니다: " + path + " — " + e.getMessage());
        }

        log.info("문화정보원 CSV 파싱 완료 — 사용 {}건, 제외 {}건", result.size(), skipped);

        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.SYNC_SOURCE_UNAVAILABLE,
                    "CSV 를 읽었지만 사용 가능한 행이 0건입니다 (제외 " + skipped + "건). "
                            + "인코딩(현재 " + charset + ")이 맞는지, 위도·경도 컬럼에 값이 있는지 확인하세요");
        }
        return result;
    }

    private Map<String, Integer> readHeader(CSVReader reader, Charset charset)
            throws IOException, CsvValidationException {

        String[] columns = reader.readNext();
        if (columns == null) {
            throw new BusinessException(ErrorCode.SYNC_SOURCE_UNAVAILABLE, "CSV 헤더가 비어 있습니다");
        }

        Map<String, Integer> header = new LinkedHashMap<>();
        for (int i = 0; i < columns.length; i++) {
            // BOM 과 앞뒤 공백을 떼야 "시설명" 과 "\uFEFF시설명" 이 같은 키가 된다.
            header.put(columns[i].replace("\uFEFF", "").trim(), i);
        }

        log.info("CSV 헤더 {}개 — {}", header.size(), header.keySet());

        List<String> missing = Arrays.stream(REQUIRED_COLUMNS)
                .filter(column -> !header.containsKey(column))
                .toList();

        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.SYNC_SOURCE_UNAVAILABLE,
                    "필수 컬럼을 찾지 못했습니다: " + missing
                            + " / 읽어낸 헤더: " + header.keySet()
                            + " / 현재 인코딩: " + charset
                            + " — 헤더가 깨져 보이면 external.culture-data.charset 을 바꾸세요");
        }
        return header;
    }

    private PlaceSyncData toSyncData(String[] row, Map<String, Integer> header) {
        Double latitude = parseDouble(value(row, header, COL_LATITUDE));
        Double longitude = parseDouble(value(row, header, COL_LONGITUDE));

        String address = resolveAddress(row, header);
        String name = value(row, header, COL_NAME);
        if (name == null || latitude == null || longitude == null) {
            return null;
        }

        return new PlaceSyncData(
                SourceType.CULTURE_DATA,
                // 이 데이터셋에는 고유 ID 가 없다. 이름+좌표로 만든 키가 곧 식별자가 된다.
                externalId(name, latitude, longitude),
                name,
                CategoryMapper.fromCultureData(
                        value(row, header, COL_CATEGORY2),
                        value(row, header, COL_CATEGORY3)),
                address,
                value(row, header, COL_PHONE),
                latitude,
                longitude,
                null,
                PetPolicyParser.parse(
                        value(row, header, COL_PET_ALLOWED),
                        value(row, header, COL_PET_SIZE),
                        value(row, header, COL_RESTRICTION),
                        value(row, header, COL_INDOOR),
                        value(row, header, COL_OUTDOOR),
                        value(row, header, COL_EXTRA_FEE))
        );
    }

    /**
     * 도로명주소 → 지번주소 → 행정구역 조합 순으로 찾는다.
     * 주소가 비어 있다고 버리면 좌표는 멀쩡한 장소까지 사라진다.
     */
    private String resolveAddress(String[] row, Map<String, Integer> header) {
        String road = value(row, header, COL_ROAD_ADDRESS);
        if (road != null) {
            return road;
        }
        String lot = value(row, header, COL_LOT_ADDRESS);
        if (lot != null) {
            return lot;
        }

        StringBuilder composed = new StringBuilder();
        for (String column : new String[]{COL_SIDO, COL_SIGUNGU, COL_EUPMYEONDONG, COL_RI, COL_BUNJI}) {
            String part = value(row, header, column);
            if (part != null) {
                composed.append(composed.isEmpty() ? "" : " ").append(part);
            }
        }
        return composed.isEmpty() ? null : composed.toString();
    }

    /**
     * 소수점 5자리(약 1m)까지만 쓴다. 좌표가 미세하게 흔들려
     * 갱신 때마다 같은 장소가 새 행으로 들어오는 것을 막는다.
     * <p>
     * 이름을 60자로 자르는 이유: place.external_id 가 VARCHAR(100) 이다.
     * 좌표 두 개가 약 22자를 쓰므로 이름이 길면 컬럼 길이를 넘겨 INSERT 가 실패한다.
     * 같은 이름이 60자까지 똑같으면서 좌표도 1m 안에 있는 시설은 사실상 같은 곳이다.
     */
    private String externalId(String name, double latitude, double longitude) {
        String trimmed = name.trim();
        if (trimmed.length() > 60) {
            trimmed = trimmed.substring(0, 60);
        }
        return "%s_%.5f_%.5f".formatted(trimmed, latitude, longitude);
    }

    private String value(String[] row, Map<String, Integer> header, String column) {
        Integer index = header.get(column);
        if (index == null || index >= row.length) {
            return null;
        }
        String value = row[index];
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double parseDouble(String value) {
        try {
            return value == null ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}