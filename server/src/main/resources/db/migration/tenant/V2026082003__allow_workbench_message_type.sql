-- Managed core evolution: allow the structured Workbench message type.
--
-- This migration only changes the storage constraint. It does not emit or
-- persist any WORKBENCH message by itself. Server emission remains gated by
-- the supported-client rollout policy.

ALTER TABLE "messages"
    DROP CONSTRAINT "messages_type_check";

ALTER TABLE "messages"
    ADD CONSTRAINT "messages_type_check"
    CHECK ((("type")::"text" = ANY (ARRAY[
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
