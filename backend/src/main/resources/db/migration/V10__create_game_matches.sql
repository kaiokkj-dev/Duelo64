CREATE TABLE game_matches (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES game_rooms(id),
    game_type VARCHAR(32) NOT NULL,
    room_code VARCHAR(6) NOT NULL,
    white_player_id UUID NOT NULL REFERENCES users(id),
    black_player_id UUID NOT NULL REFERENCES users(id),
    winner_id UUID REFERENCES users(id),
    loser_id UUID REFERENCES users(id),
    result VARCHAR(16) NOT NULL,
    finish_reason VARCHAR(32) NOT NULL,
    time_control_minutes INTEGER NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_millis BIGINT NOT NULL,
    move_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_game_matches_room_id UNIQUE (room_id)
);

CREATE INDEX idx_game_matches_white_finished ON game_matches (white_player_id, finished_at DESC);
CREATE INDEX idx_game_matches_black_finished ON game_matches (black_player_id, finished_at DESC);
