# Duelo64

Plataforma web multiplayer de jogos de estratégia em tempo real.

O Duelo64 foi criado para oferecer partidas competitivas e salas privadas em jogos clássicos de tabuleiro, com backend autoritativo, matchmaking, ranking, histórico e comunicação em tempo real.

Atualmente, a plataforma possui:

- Damas Brasileiras
- Xadrez

## Funcionalidades

### Multiplayer

- Salas privadas por código
- Matchmaking público ranqueado
- WebSocket/STOMP em tempo real
- Reconexão automática
- Presença online durante partidas
- Chat em tempo real
- Revanche
- Oferta e aceitação de empate
- Abandono de partida

### Competitivo

- Elo independente por modalidade
- Ranking público
- Estatísticas de jogador
- Histórico de partidas
- Variação de Elo por partida ranqueada
- Perfis públicos

### Damas Brasileiras

Motor de regras executado no backend com:

- captura obrigatória
- Lei da Maioria
- captura múltipla
- promoção para dama
- movimento e captura de dama
- vitória por falta de peças
- vitória por falta de movimentos
- empate por repetição
- empate por limite de movimentos
- cronômetro e derrota por tempo

### Xadrez

Motor completo no backend com:

- movimentos legais de todas as peças
- xeque
- xeque-mate
- roque
- en passant
- promoção
- afogamento
- repetição de posição
- regra dos 50 lances
- material insuficiente
- cronômetro e derrota por tempo

## Stack

### Frontend

- HTML5
- CSS3
- JavaScript
- WebSocket / STOMP

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

## Arquitetura

O backend é a autoridade das partidas.

O frontend não decide se uma jogada é válida. Ele envia a ação para o servidor, que valida as regras, persiste o novo estado e publica a atualização para os jogadores via WebSocket.

A infraestrutura multiplayer é compartilhada entre as modalidades:

- salas
- matchmaking
- ranking
- Elo
- histórico
- presença
- chat
- reconexão
- revanche

Cada jogo possui seu próprio domínio de regras:

```text
game/
├── checkers/
├── chess/
├── matchmaking/
├── match/
└── room/
