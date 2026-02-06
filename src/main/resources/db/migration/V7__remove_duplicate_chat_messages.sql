-- Remove duplicate chat messages caused by double MessageChatMemoryAdvisor configuration
-- This migration keeps only one instance of each duplicate message

-- Create a temporary table with unique messages
CREATE TEMP TABLE temp_unique_messages AS
SELECT DISTINCT ON (conversation_id, content, type) *
FROM spring_ai_chat_memory
ORDER BY conversation_id, content, type, "timestamp";

-- Clear the original table
TRUNCATE spring_ai_chat_memory;

-- Restore unique messages
INSERT INTO spring_ai_chat_memory (conversation_id, content, type, "timestamp")
SELECT conversation_id, content, type, "timestamp"
FROM temp_unique_messages
ORDER BY "timestamp";

-- Clean up temp table
DROP TABLE temp_unique_messages;
