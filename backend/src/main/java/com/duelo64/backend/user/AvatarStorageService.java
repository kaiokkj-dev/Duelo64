package com.duelo64.backend.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class AvatarStorageService {

    private static final long MAX_FILE_SIZE = 2L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Cloudinary cloudinary;

    public AvatarStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String upload(UUID userId, MultipartFile file) {
        validate(file);

        Map<String, Object> uploadOptions = ObjectUtils.asMap(
                "folder", "duelo64/avatars",
                "public_id", "user-" + userId,
                "resource_type", "image",
                "overwrite", true,
                "invalidate", true,
                "transformation", "c_fill,g_auto,h_256,w_256"
        );

        try {
            Map<?, ?> uploadResult = cloudinary
                    .uploader()
                    .upload(file.getBytes(), uploadOptions);

            Object secureUrl = uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new AvatarUploadException();
            }

            return secureUrl.toString();
        } catch (IOException exception) {
            throw new AvatarUploadException();
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAvatarException("Selecione uma imagem para enviar.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidAvatarException("A imagem deve possuir no máximo 2 MB.");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new InvalidAvatarException("Envie uma imagem JPEG, PNG ou WebP.");
        }

        if (!hasValidImageSignature(file)) {
            throw new InvalidAvatarException("O conteúdo do arquivo não corresponde a uma imagem válida.");
        }
    }

    private boolean hasValidImageSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);

            return isJpeg(header) || isPng(header) || isWebp(header);
        } catch (IOException exception) {
            throw new InvalidAvatarException("Não foi possível ler a imagem enviada.");
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && unsigned(header[0]) == 0xFF
                && unsigned(header[1]) == 0xD8
                && unsigned(header[2]) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        if (header.length < signature.length) {
            return false;
        }

        for (int index = 0; index < signature.length; index++) {
            if (unsigned(header[index]) != signature[index]) {
                return false;
            }
        }

        return true;
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P';
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }
}
