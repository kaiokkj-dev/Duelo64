CREATE TABLE auth_codes (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_auth_codes_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_auth_codes_email_created_at
    ON auth_codes (LOWER(email), created_at DESC);

CREATE INDEX idx_auth_codes_expires_at
    ON auth_codes (expires_at);