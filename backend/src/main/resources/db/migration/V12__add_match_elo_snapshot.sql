ALTER TABLE game_matches
    ADD COLUMN white_rating_before INTEGER,
    ADD COLUMN white_rating_after INTEGER,
    ADD COLUMN black_rating_before INTEGER,
    ADD COLUMN black_rating_after INTEGER;
