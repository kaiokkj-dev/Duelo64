# Duelo64 API

Backend da plataforma **Duelo64**, desenvolvido com Java e Spring Boot.

A API é responsável por autenticação, regras dos jogos, salas multiplayer, matchmaking ranqueado, Elo, histórico, cronômetros e comunicação em tempo real.

## Tecnologias

- Java
- Spring Boot
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Spring WebSocket / STOMP
- JWT
- PostgreSQL
- Neon
- Flyway
- Maven Wrapper

## Funcionalidades principais

- Autenticação por código de e-mail
- Sessão com JWT
- Salas privadas
- Matchmaking ranqueado
- Elo independente por modalidade
- Histórico de partidas
- Ranking
- WebSocket/STOMP
- Presença e reconexão
- Chat em tempo real
- Cronômetros autoritativos
- Revanche

### Jogos

- Damas Brasileiras
- Xadrez

As regras são validadas pelo backend. O frontend nunca é a autoridade sobre uma jogada.

## Banco de dados

O projeto utiliza PostgreSQL hospedado no Neon.

As alterações de schema são controladas através de migrations do Flyway.

Use o arquivo:

```text
.env.example
