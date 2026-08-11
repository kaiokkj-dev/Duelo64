package com.duelo64.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;

@Service
public class AuthEmailService {

    private final Resend resend;
    private final String fromEmail;

    public AuthEmailService(
            Resend resend,
            @Value("${RESEND_FROM_EMAIL}") String fromEmail) {

        this.resend = resend;
        this.fromEmail = fromEmail;
    }

    public void sendCode(String recipientEmail, String code) {
        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(recipientEmail)
                .subject("Seu código de acesso ao Duelo64")
                .html(buildEmailHtml(code))
                .build();

        try {
            resend.emails().send(email);
        } catch (ResendException exception) {
            throw new IllegalStateException(
                    "Não foi possível enviar o código por e-mail.",
                    exception
            );
        }
    }

private String buildEmailHtml(String code) {
    return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <body style="
                margin: 0;
                padding: 0;
                background-color: #12100f;
                font-family: Arial, Helvetica, sans-serif;
            ">
                <table
                    role="presentation"
                    width="100%%"
                    cellspacing="0"
                    cellpadding="0"
                    style="background-color: #12100f; padding: 40px 16px;"
                >
                    <tr>
                        <td align="center">
                            <table
                                role="presentation"
                                width="100%%"
                                cellspacing="0"
                                cellpadding="0"
                                style="
                                    max-width: 520px;
                                    background-color: #1a1715;
                                    border: 1px solid #332b27;
                                    border-radius: 7px;
                                "
                            >
                                <tr>
                                    <td style="padding: 36px;">
                                        <p style="
                                            margin: 0 0 28px;
                                            color: #ff6b0b;
                                            font-size: 22px;
                                            font-weight: 800;
                                            letter-spacing: 1px;
                                        ">
                                            DUELO<span style="font-size: 15px;">64</span>
                                        </p>
                                        <h1 style="
                                            margin: 0 0 14px;
                                            color: #f8f5f2;
                                            font-size: 25px;
                                            line-height: 1.3;
                                        ">
                                            Seu acesso está quase pronto
                                        </h1>
                                        <p style="
                                            margin: 0 0 28px;
                                            color: #aaa19b;
                                            font-size: 15px;
                                            line-height: 1.6;
                                        ">
                                            Digite o código abaixo para confirmar
                                            seu acesso ao Duelo64.
                                        </p>
                                        <div style="
                                            background-color: #120f0e;
                                            border: 1px solid #46382f;
                                            border-left: 4px solid #ff6b0b;
                                            border-radius: 7px;
                                            padding: 22px 16px;
                                            text-align: center;
                                        ">
                                            <span style="
                                                color: #ffffff;
                                                font-size: 34px;
                                                font-weight: 800;
                                                letter-spacing: 10px;
                                            ">
                                                %s
                                            </span>
                                        </div>
                                        <p style="
                                            margin: 24px 0 0;
                                            color: #aaa19b;
                                            font-size: 14px;
                                            line-height: 1.6;
                                        ">
                                            Este código expira em
                                            <strong style="color: #f8f5f2;">
                                                5 minutos
                                            </strong>.
                                        </p>
                                        <div style="
                                            height: 1px;
                                            margin: 30px 0;
                                            background-color: #332b27;
                                        "></div>
                                        <p style="
                                            margin: 0;
                                            color: #736b66;
                                            font-size: 12px;
                                            line-height: 1.6;
                                        ">
                                            Se você não solicitou este acesso,
                                            pode ignorar esta mensagem com segurança.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                            <p style="
                                margin: 18px 0 0;
                                color: #5f5853;
                                font-size: 11px;
                            ">
                                Duelo64 — pense, mova e vença.
                            </p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(code);
}
}
