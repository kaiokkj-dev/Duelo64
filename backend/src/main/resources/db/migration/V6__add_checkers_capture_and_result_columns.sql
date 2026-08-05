ALTER TABLE checkers_game_states
    ADD COLUMN forced_capture_row INTEGER,
    ADD COLUMN forced_capture_column INTEGER,
    ADD COLUMN winner_color VARCHAR(16),
    ADD COLUMN finish_reason VARCHAR(32);
