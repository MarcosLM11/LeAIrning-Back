CREATE TABLE quizzs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    document_id UUID NOT NULL,
    quizz TEXT NOT NULL,
    last_score INT NOT NULL DEFAULT 0,
    version BIGINT,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quizzs_user_id ON quizzs(user_id);
CREATE INDEX idx_quizzs_document_id ON quizzs(document_id);