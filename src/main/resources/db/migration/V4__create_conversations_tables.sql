-- Create conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create junction table for conversation-documents relationship
CREATE TABLE conversation_documents (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, document_id)
);

-- Create indexes for performance
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_updated ON conversations(last_updated_timestamp DESC);
CREATE INDEX idx_conversation_documents_document_id ON conversation_documents(document_id);
