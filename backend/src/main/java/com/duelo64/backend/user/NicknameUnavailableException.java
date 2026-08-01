package com.duelo64.backend.user;

public class NicknameUnavailableException extends RuntimeException {

    public NicknameUnavailableException() {
        super("Este nickname já está sendo utilizado.");
    }
}