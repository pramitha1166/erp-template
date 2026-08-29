-- ARCH-1 / ARCH-7: the Spring Modulith event-publication registry. Every
-- cross-module @ApplicationModuleListener (audit's AuthAuditEventListener,
-- ...) makes the framework persist a row here at publication time and
-- complete it once the listener has run, so without these tables *any*
-- request that publishes a module event fails at commit. The schema is
-- owned by spring-modulith-events-jpa (JpaEventPublication and its two
-- subclasses); it is reproduced here rather than generated because Flyway
-- owns the schema in every environment, including local dev (ARCH-7) —
-- keep it in step when the spring-modulith version moves.
--
-- ARCH-2 exception: these two tables carry no tenant_id and therefore no
-- RLS policy. The columns are fixed by the framework's entity mapping, so
-- a tenant_id could never be populated on insert. Nothing reads them but
-- Modulith's own registry — no application query, and no API surface —
-- and the tenant a publication belongs to is inside its serialized event,
-- which only the originating listener ever deserializes.
CREATE TABLE event_publication (
    id               uuid PRIMARY KEY,
    listener_id      text NOT NULL,
    event_type       text NOT NULL,
    serialized_event text NOT NULL,
    publication_date timestamptz NOT NULL,
    completion_date  timestamptz
);

-- Republishing incomplete publications on restart scans by completion_date;
-- resolving a publication for completion looks it up by its event/listener
-- pair. Both are the registry's hot paths.
CREATE INDEX idx_event_publication_completion_date ON event_publication (completion_date);
CREATE INDEX idx_event_publication_listener_id_serialized_event
    ON event_publication (listener_id, serialized_event);

-- Completed publications are moved here when the archiving completion mode
-- is in use, keeping the live table small.
CREATE TABLE event_publication_archive (
    id               uuid PRIMARY KEY,
    listener_id      text NOT NULL,
    event_type       text NOT NULL,
    serialized_event text NOT NULL,
    publication_date timestamptz NOT NULL,
    completion_date  timestamptz
);

CREATE INDEX idx_event_publication_archive_listener_id_serialized_event
    ON event_publication_archive (listener_id, serialized_event);
