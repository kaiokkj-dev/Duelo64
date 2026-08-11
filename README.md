# Duelo64

Plataforma web multiplayer de jogos de estratégia em tempo real.

O **Duelo64** oferece partidas competitivas e salas privadas em jogos clássicos de tabuleiro. A plataforma conta com backend autoritativo, matchmaking ranqueado, sistema Elo, ranking público, histórico de partidas e comunicação em tempo real.

Atualmente, estão disponíveis:

- Damas Brasileiras
- Xadrez

---

## Funcionalidades

### Multiplayer

- Salas privadas compartilhadas por código
- Matchmaking público ranqueado
- Atualização das partidas em tempo real
- Comunicação com WebSocket e STOMP
- Reconexão automática
- Presença online durante as partidas
- Chat em tempo real
- Cronômetros sincronizados pelo servidor
- Oferta e aceitação de empate
- Abandono de partida
- Revanche com inversão de cores

### Sistema competitivo

- Partidas `FRIENDLY` e `RANKED`
- Elo independente para cada modalidade
- Matchmaking por faixa de Elo
- Ranking público de Damas e Xadrez
- Estatísticas por modalidade
- Histórico de partidas
- Variação de Elo por partida ranqueada
- Perfis públicos de jogadores

### Conta e perfil

- Autenticação sem senha
- Código de acesso com 6 dígitos enviado por e-mail
- Autorização por JWT
- Nickname exclusivo
- Avatar personalizado
- Upload de imagem
- Perfil próprio editável
- Perfil público de outros jogadores

---

## Damas Brasileiras

O Duelo64 possui um motor próprio de Damas Brasileiras executado integralmente no backend.

O frontend apenas apresenta o estado recebido do servidor. Toda jogada é validada antes de ser persistida e enviada ao adversário.

### Regras implementadas

- Movimento de pedras
- Captura obrigatória
- Captura para frente e para trás
- Lei da Maioria
- Captura múltipla
- Continuação obrigatória da captura
- Promoção para dama
- Movimento de damas
- Captura de damas
- Vitória quando o adversário fica sem peças
- Vitória quando o adversário fica sem movimentos legais
- Empate por acordo
- Empate por repetição de posição
- Empate por limite de movimentos
- Abandono
- Derrota por tempo

---

## Xadrez

O Xadrez possui um motor de regras próprio e independente do motor de Damas.

O servidor valida os movimentos, controla o estado da partida e impede qualquer jogada que deixe o próprio rei em xeque.

### Regras implementadas

- Movimento de peões
- Movimento de cavalos
- Movimento de bispos
- Movimento de torres
- Movimento da rainha
- Movimento do rei
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

---

## Autenticação

O Duelo64 utiliza autenticação sem senha.

O fluxo funciona da seguinte forma:

```text
Usuário informa o e-mail
        ↓
Backend gera um código de 6 dígitos
        ↓
Código é armazenado com hash e data de expiração
        ↓
Código é enviado por e-mail
        ↓
Usuário confirma o código
        ↓
Backend emite um JWT
```

### Proteções implementadas

- Código gerado com `SecureRandom`
- Código armazenado com hash
- Expiração do código
- Código de uso único
- Limite de tentativas
- Rate limit para solicitação e verificação
- JWT com expiração
- Respostas de autenticação sem exposição do código

---

## Arquitetura

O backend é a autoridade das partidas.

O frontend não decide se uma jogada é válida e não modifica sozinho o estado oficial do jogo.

```text
Jogador realiza uma ação
        ↓
Frontend envia a ação ao backend
        ↓
Servidor autentica o jogador
        ↓
Servidor valida permissões e regras
        ↓
Estado oficial é persistido no PostgreSQL
        ↓
Evento é publicado via WebSocket/STOMP
        ↓
Os jogadores recebem o estado atualizado
```

A plataforma mantém separados os motores específicos de cada jogo e reutiliza a infraestrutura compartilhada.

```text
Infraestrutura compartilhada
├── Autenticação
├── Usuários e perfis
├── Salas
├── Matchmaking
├── Elo e ranking
├── Histórico
├── WebSocket
├── Presença
├── Chat
├── Cronômetros
└── Revanche

Motores de jogos
├── Damas Brasileiras
└── Xadrez
```

Essa organização permite adicionar novas modalidades sem reescrever toda a plataforma.

---

## Stack

### Frontend

- HTML5
- CSS3
- JavaScript
- WebSocket
- STOMP
- Lucide Icons

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Spring Web MVC
- Spring WebSocket
- JWT
- Maven

### Banco de dados e migrations

- PostgreSQL
- Neon
- Flyway

### Serviços externos

- Cloudinary para armazenamento de avatares
- Resend para envio dos códigos de acesso

---

## Segurança

Entre as proteções implementadas estão:

- Backend autoritativo
- Autenticação das requisições com JWT
- Autenticação das conexões WebSocket
- Autorização dos tópicos privados das salas
- Validação de participantes
- Bloqueio de acesso externo ao estado das partidas
- Rate limit em rotas sensíveis
- Limite de tamanho das mensagens do chat
- Renderização segura das mensagens
- Validação de MIME e assinatura dos avatares
- Limite de tamanho para upload
- Códigos de autenticação armazenados com hash
- Segredos mantidos em variáveis de ambiente
- DTOs específicos para evitar exposição de informações privadas

---

## Estrutura do projeto

```text
Duelo64/
├── backend/
│   ├── src/main/java/
│   │   └── com/duelo64/backend/
│   │       ├── auth/
│   │       ├── game/
│   │       │   ├── checkers/
│   │       │   ├── chess/
│   │       │   ├── match/
│   │       │   ├── matchmaking/
│   │       │   ├── room/
│   │       │   └── stats/
│   │       ├── shared/
│   │       └── user/
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   └── application.yml
│   ├── src/test/
│   ├── .env.example
│   └── pom.xml
│
└── public/
    ├── assets/
    ├── checkers/
    ├── chess/
    ├── shared/
    ├── index.html
    ├── perfil.html
    └── ranking.html
```

---

## Banco de dados

O projeto utiliza PostgreSQL com migrations versionadas pelo Flyway.

As principais estruturas armazenadas incluem:

- Usuários
- Códigos de autenticação
- Salas
- Estados das partidas de Damas
- Estados das partidas de Xadrez
- Histórico de partidas
- Ratings separados por modalidade

O banco hospedado utilizado no projeto é o Neon.

---

## Variáveis de ambiente

Copie o arquivo `.env.example` para `.env` dentro da pasta `backend`:

```powershell
Copy-Item .env.example .env
```

Configure as variáveis:

```env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=

RESEND_API_KEY=
RESEND_FROM_EMAIL=

JWT_SECRET=
JWT_EXPIRATION_MINUTES=60
JWT_ISSUER=duelo64-api

CLOUDINARY_URL=

ALLOWED_ORIGINS=http://127.0.0.1:5500,http://localhost:5500
SERVER_PORT=8080
```

O `JWT_SECRET` deve ser um segredo forte codificado em Base64.

Nenhuma credencial real deve ser enviada ao repositório.

---

## Como executar localmente

### Requisitos

- Java 21
- PostgreSQL ou banco Neon
- Navegador moderno

### Clonar o projeto

```bash
git clone https://github.com/seu-usuario/duelo64.git
cd duelo64
```

### Configurar o backend

```powershell
cd backend
Copy-Item .env.example .env
```

Preencha as variáveis do arquivo `.env`.

### Executar os testes

```powershell
.\mvnw.cmd test
```

### Iniciar o backend

```powershell
.\mvnw.cmd spring-boot:run
```

A API ficará disponível em:

```text
http://localhost:8080
```

Para verificar:

```text
GET http://localhost:8080/api/v1/status
```

### Iniciar o frontend

Abra a pasta `public` utilizando um servidor local, como a extensão Live Server do VS Code.

Exemplo:

```text
http://127.0.0.1:5500/public/index.html
```

---

## Testes

O projeto possui testes automatizados para:

- Autenticação
- Rate limit
- Regras de Damas Brasileiras
- Captura obrigatória
- Lei da Maioria
- Captura múltipla
- Regras fundamentais do Xadrez
- Xeque e xeque-mate
- Roque
- En passant
- Promoção
- Condições automáticas de empate
- Salas
- Matchmaking
- Elo
- Ranking
- Histórico
- Estatísticas
- Perfis públicos

Na última validação, a suíte possuía:

```text
149 testes
0 falhas
0 erros
```

---

## Status

A V1 do Duelo64 está funcionalmente concluída.

Antes da publicação definitiva, restam configurações de infraestrutura e deploy, incluindo:

- URL pública do backend
- Configuração da origem da API no frontend
- CORS de produção
- Domínio e remetente verificado no Resend
- Variáveis de ambiente nos serviços de hospedagem
- Validação das migrations em um banco PostgreSQL limpo

---

## Possíveis evoluções futuras

- Novas modalidades de estratégia
- Sistema de amigos
- Convites entre jogadores
- Replay das partidas
- Espectadores
- Torneios
- Conquistas
- Moderação de chat
- Aplicativo mobile

---

## Autor

Desenvolvido por **Kaio Henrique**.

- Portfólio: [kaiohenrique.dev](https://kaiohenrique.dev)
- E-mail: kaiohenriquemalaquias@gmail.com
