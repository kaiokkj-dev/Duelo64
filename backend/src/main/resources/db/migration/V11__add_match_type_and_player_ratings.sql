ALTER TABLE game_rooms
    ADD COLUMN match_type VARCHAR(16) NOT NULL DEFAULT 'FRIENDLY';

ALTER TABLE game_matches
    ADD COLUMN match_type VARCHAR(16) NOT NULL DEFAULT 'FRIENDLY',
    ADD COLUMN elo_processed BOOLEAN NOT NULL DEFAULT FALSE;

-- Todas as partidas existentes foram criadas pelas salas privadas atuais.
UPDATE game_matches SET elo_processed = TRUE WHERE match_type = 'FRIENDLY';

CREATE TABLE player_game_ratings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    game_type VARCHAR(32) NOT NULL,
    rating INTEGER NOT NULL DEFAULT 1000 CHECK (rating >= 100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_player_game_ratings_user_game UNIQUE (user_id, game_type)
);

CREATE INDEX idx_player_game_ratings_game_rating
    ON player_game_ratings (game_type, rating DESC);
