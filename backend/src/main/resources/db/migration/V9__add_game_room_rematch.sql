ALTER TABLE game_rooms
    ADD COLUMN rematch_requested_by_user_id UUID REFERENCES users(id),
    ADD COLUMN rematch_room_code VARCHAR(6) REFERENCES game_rooms(code);

CREATE UNIQUE INDEX uk_game_rooms_rematch_room_code
    ON game_rooms (rematch_room_code)
    WHERE rematch_room_code IS NOT NULL;
