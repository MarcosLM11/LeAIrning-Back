-- Create SPRING_AI_CHAT_MEMORY table for persistent chat history
-- This table is normally created by Spring AI auto-configuration, but we manage it
-- via Flyway to customize the conversation_id column size.
--
-- We use a composite conversation ID format: userId_conversationId
-- Example: ba598876-3533-4afa-8919-d676fd6167ab_1e790f71-305e-4d55-aaf2-d1111186e92b
-- This requires VARCHAR(100) instead of the default VARCHAR(36)

CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    conversation_id VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
ON spring_ai_chat_memory(conversation_id, "timestamp");

COMMENT ON TABLE spring_ai_chat_memory IS 'Stores chat message history for Spring AI ChatMemory';
COMMENT ON COLUMN spring_ai_chat_memory.conversation_id IS 'Composite key: userId_conversationId';
