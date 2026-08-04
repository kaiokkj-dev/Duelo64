CREATE TABLE game_rooms (
    id UUID PRIMARY KEY,
    code VARCHAR(6) NOT NULL,
    game_type VARCHAR(32) NOT NULL,
    room_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    time_control_minutes INTEGER NOT NULL,
    host_user_id UUID NOT NULL REFERENCES users(id),
    guest_user_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uk_game_rooms_code
    ON game_rooms (code);

CREATE INDEX idx_game_rooms_status
    ON game_rooms (status);

CREATE INDEX idx_game_rooms_host_user_id
    ON game_rooms (host_user_id);

CREATE INDEX idx_game_rooms_guest_user_id
    ON game_rooms (guest_user_id);
