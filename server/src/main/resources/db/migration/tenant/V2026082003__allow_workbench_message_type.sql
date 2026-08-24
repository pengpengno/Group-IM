-- Managed core evolution after the immutable 2026081906 adoption baseline.
-- This migration does not emit or create any WORKBENCH messages. It only makes
-- future persistence legal once the client/release gates are satisfied.

ALTER TABLE "messages"
    DROP CONSTRAINT "messages_type_check";

ALTER TABLE "messages"
    ADD CONSTRAINT "messages_type_check" CHECK ((("type")::"text" = ANY (ARRAY[
        ('TEXT'::character varying)::"text",
        ('FILE'::character varying)::"text",
        ('VOICE'::character varying)::"text",
        ('VIDEO'::character varying)::"text",
        ('IMAGE'::character varying)::"text",
        ('MEDIA'::character varying)::"text",
        ('MEETING'::character varying)::"text",
        ('BOT_CARD'::character varying)::"text",
        ('WORKBENCH'::character varying)::"text"
    ])));

INSERT INTO tenant_schema_metadata(metadata_key, metadata_value)
VALUES ('managed_core_contract', '2026082003')
ON CONFLICT (metadata_key) DO UPDATE
SET metadata_value = EXCLUDED.metadata_value,
    updated_at = NOW();
