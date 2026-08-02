package com.duelo64.backend.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {

    @Bean
    Cloudinary cloudinary(
            @Value("${CLOUDINARY_URL}") String cloudinaryUrl) {

        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        cloudinary.config.secure = true;

        return cloudinary;
    }
}
