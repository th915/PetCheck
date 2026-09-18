package com.petcheck.backend.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * MVP 이미지 저장소.
 * <p>
 * Google Drive API 는 쓰지 않는다. 서비스 계정에는 자체 저장 용량이 없어서
 * 개인 드라이브 폴더에 업로드하면 storageQuotaExceeded 로 실패하기 때문이다.
 * 배포 단계에서 클라우드로 옮길 때는 이 클래스만 교체하면 된다.
 */
@Service
public class LocalImageStorage {

    private final Path uploadRoot;

    public LocalImageStorage(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath();
    }

    /**
     * 검증까지 여기서 한다. 업로드 경로가 늘어날 때마다 같은 검사를 복사하면
     * 한 곳이 빠져도 아무도 모른다.
     */
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE, "이미지가 아닙니다: " + contentType);
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext == null ? "" : "." + ext);
        Path target = uploadRoot.resolve(filename);

        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, e.getMessage());
        }

        return "/uploads/" + filename;
    }

    /**
     * 저장된 이미지를 다시 읽는다. url 은 save() 가 돌려준 "/uploads/파일명" 형태다.
     * 파일명만 잘라 쓰기 때문에 경로 조작으로 상위 디렉터리를 읽을 수 없다.
     */
    public byte[] read(String url) throws IOException {
        String filename = Paths.get(url).getFileName().toString();
        return Files.readAllBytes(uploadRoot.resolve(filename));
    }

    public String mimeTypeOf(String url) {
        String ext = StringUtils.getFilenameExtension(url);
        if (ext == null) {
            return "image/jpeg";
        }
        return switch (ext.toLowerCase()) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            default -> "image/jpeg";
        };
    }
}
