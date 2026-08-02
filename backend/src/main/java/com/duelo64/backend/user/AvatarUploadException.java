package com.duelo64.backend.user;

public class AvatarUploadException extends RuntimeException {

    public AvatarUploadException() {
        super("Não foi possível enviar a imagem agora. Tente novamente.");
    }
}
