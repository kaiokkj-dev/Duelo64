ALTER TABLE checkers_game_states
    ADD COLUMN position_occurrences TEXT NOT NULL DEFAULT '',
    ADD COLUMN king_only_move_count INTEGER NOT NULL DEFAULT 0;
