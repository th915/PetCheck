package com.petcheck.backend.service.place;

/**
 * @param inserted 새로 만든 장소 수
 * @param updated  이미 있어서 정보만 갱신한 장소 수
 * @param skipped  좌표나 이름이 없어 버린 행 수
 */
public record SyncResult(int inserted, int updated, int skipped) {

    public static SyncResult empty() {
        return new SyncResult(0, 0, 0);
    }

    /** 청크 결과를 누적한다. record 는 불변이므로 더한 새 값을 돌려준다. */
    public SyncResult plus(SyncResult other) {
        return new SyncResult(
                inserted + other.inserted,
                updated + other.updated,
                skipped + other.skipped);
    }

    public int total() {
        return inserted + updated + skipped;
    }
}