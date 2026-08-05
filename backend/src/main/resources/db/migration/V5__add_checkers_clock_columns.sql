ALTER TABLE checkers_game_states
    ADD COLUMN white_remaining_millis BIGINT NOT NULL DEFAULT 600000,
    ADD COLUMN black_remaining_millis BIGINT NOT NULL DEFAULT 600000,
    ADD COLUMN turn_started_at TIMESTAMP WITH TIME ZONE;

UPDATE checkers_game_states
SET turn_started_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE turn_started_at IS NULL;
