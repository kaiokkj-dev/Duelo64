# Duelo64 API

Backend da plataforma Duelo64, desenvolvido com Java e Spring Boot.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven Wrapper

## Banco de dados

A aplicação aceita PostgreSQL local ou hospedado no Supabase. Use o arquivo
`.env.example` como referência e nunca envie credenciais reais ao GitHub.

## Comandos

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Com o PostgreSQL configurado, a API responde em:

```text
GET http://localhost:8080/api/v1/status
```
