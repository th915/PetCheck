package com.petcheck.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 업로드된 이미지를 /uploads/** 로 서빙한다.
 * DB 에는 "/uploads/{uuid}.jpg" 상대 경로만 저장하므로
 * 저장소를 Firebase Storage 나 S3 로 바꿔도 DB 값 형식은 그대로 쓸 수 있다.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
