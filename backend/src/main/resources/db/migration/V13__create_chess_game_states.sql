CREATE TABLE chess_game_states (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL UNIQUE REFERENCES game_rooms(id) ON DELETE CASCADE,
    board_fen VARCHAR(128) NOT NULL,
    current_turn VARCHAR(16) NOT NULL,
    move_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_chess_game_states_room_id ON chess_game_states(room_id);
