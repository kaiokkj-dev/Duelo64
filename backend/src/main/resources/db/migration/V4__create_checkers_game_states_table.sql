CREATE TABLE checkers_game_states (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES game_rooms(id),
    board_notation TEXT NOT NULL,
    current_turn VARCHAR(16) NOT NULL,
    move_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_checkers_game_states_room_id
    ON checkers_game_states (room_id);
