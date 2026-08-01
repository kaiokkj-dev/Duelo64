package com.duelo64.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "O nickname é obrigatório.")
        @Size(
                min = 3,
                max = 24,
                message = "O nickname deve possuir entre 3 e 24 caracteres."
        )
        @Pattern(
                regexp = "^[A-Za-z0-9_]+$",
                message = "Use somente letras, números e underline."
        )
        String nickname

) {
}