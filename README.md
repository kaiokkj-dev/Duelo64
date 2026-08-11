# Duelo64

Plataforma web multiplayer de jogos de estratégia em tempo real.

O **Duelo64** foi criado para oferecer partidas competitivas e salas privadas em jogos clássicos de tabuleiro, com backend autoritativo, matchmaking ranqueado, Elo, ranking, histórico e comunicação em tempo real.

Atualmente, a plataforma possui:

- Damas Brasileiras
- Xadrez

---

## Funcionalidades

### Multiplayer

- Salas privadas por código
- Matchmaking público ranqueado
- Atualização em tempo real com WebSocket/STOMP
- Reconexão automática
- Presença online durante partidas
- Chat em tempo real
- Revanche
- Oferta e aceitação de empate
- Abandono de partida
- Cronômetros sincronizados pelo servidor

### Competitivo

- Elo independente por modalidade
- Ranking público
- Estatísticas de jogador
- Histórico de partidas
- Variação de Elo por partida ranqueada
- Perfis públicos
- Partidas `FRIENDLY` e `RANKED`

---

## Damas Brasileiras

O motor de regras das Damas é executado no backend.

Principais regras implementadas:

- Movimento de pedras
- Captura obrigatória
- Captura para frente e para trás
- Lei da Maioria
- Captura múltipla
- Continuação obrigatória da captura
- Promoção para dama
- Movimento de dama
- Captura de dama
- Vitória quando o adversário fica sem peças
- Vitória quando o adversário fica sem movimentos legais
- Empate por acordo
- Empate por repetição
- Empate por limite de movimentos
- Abandono
- Derrota por tempo

---

## Xadrez

O Xadrez também possui um motor de regras independente no backend.

Principais regras implementadas:

- Movimento legal de peões
- Cavalos
- Bispos
- Torres
- Rainha
- Rei
- Capturas
- Xeque
- Xeque-mate
- Roque pequeno e grande
- En passant
- Promoção de peão
- Afogamento
- Repetição de posição
- Regra dos 50 lances
- Material insuficiente
- Empate por acordo
- Abandono
- Derrota por tempo

O servidor impede jogadas que deixariam o próprio rei em xeque.

---

## Stack

### Frontend

- HTML5
- CSS3
- JavaScript
- WebSocket
- STOMP

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Spring WebSocket
- JWT
- Flyway

### Banco e serviços

- PostgreSQL
- Neon
- Cloudinary

---

## Arquitetura

O backend é a autoridade das partidas.

O frontend não decide se uma jogada é válida.

O fluxo geral é:

```text
Jogador realiza uma ação
        ↓
Frontend envia para o backend
        ↓
Servidor valida regras e permissões
        ↓
Estado é persistido
        ↓
Evento WebSocket é publicado
        ↓
Os jogadores recebem o novo estado
