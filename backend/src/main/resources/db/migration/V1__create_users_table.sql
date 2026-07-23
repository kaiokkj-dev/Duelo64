CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    nickname VARCHAR(24),
    avatar_url VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uk_users_email_lower
    ON users (LOWER(email));

CREATE UNIQUE INDEX uk_users_nickname_lower
    ON users (LOWER(nickname))
    WHERE nickname IS NOT NULL;

