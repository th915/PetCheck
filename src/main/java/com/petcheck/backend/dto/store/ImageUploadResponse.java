package com.petcheck.backend.dto.store;

/** @param url DB 에 저장할 상대 경로. "/uploads/{uuid}.jpg" */
public record ImageUploadResponse(String url) {
}