ALTER TABLE chess_game_states ADD COLUMN white_remaining_millis BIGINT NOT NULL DEFAULT 600000;
ALTER TABLE chess_game_states ADD COLUMN black_remaining_millis BIGINT NOT NULL DEFAULT 600000;
ALTER TABLE chess_game_states ADD COLUMN turn_started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE chess_game_states ADD COLUMN draw_offered_by_color VARCHAR(16);
