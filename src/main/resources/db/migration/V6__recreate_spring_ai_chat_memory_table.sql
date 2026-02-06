-- Recreate SPRING_AI_CHAT_MEMORY table with correct schema
-- This migration fixes the conversation_id column size issue.
-- Spring AI auto-configuration created the table with VARCHAR(36), but we need VARCHAR(100)
-- for the composite conversation ID format: userId_conversationId

-- Drop the existing table (created by Spring AI or V5 with CREATE IF NOT EXISTS)
DROP TABLE IF EXISTS spring_ai_chat_memory;

-- Recreate with correct schema
CREATE TABLE spring_ai_chat_memory (
    conversation_id VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL
);

CREATE INDEX spring_ai_chat_memory_conversation_id_timestamp_idx
ON spring_ai_chat_memory(conversation_id, "timestamp");

COMMENT ON TABLE spring_ai_chat_memory IS 'Stores chat message history for Spring AI ChatMemory';
COMMENT ON COLUMN spring_ai_chat_memory.conversation_id IS 'Composite key: userId_conversationId';
