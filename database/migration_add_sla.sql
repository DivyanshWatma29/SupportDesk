-- =========================================================================
-- SupportDesk: add SLA tracking to an EXISTING MySQL database
-- Run this ONCE. Databases created from the new schema.sql already have these columns.
-- SLA hours by priority: HIGH 4, MEDIUM 24, LOW 72.
-- =========================================================================

ALTER TABLE tickets
  ADD COLUMN due_at DATETIME(6) NULL AFTER updated_at,
  ADD COLUMN resolved_at DATETIME(6) NULL AFTER due_at,
  ADD INDEX idx_ticket_status_due_at (status, due_at);

-- "updated_at = updated_at" stops MySQL from changing updated_at while we backfill.

-- Backfill due time for existing tickets: created time + SLA hours for the priority.
UPDATE tickets
SET due_at = DATE_ADD(created_at, INTERVAL CASE priority WHEN 'HIGH' THEN 4 WHEN 'MEDIUM' THEN 24 ELSE 72 END HOUR),
    updated_at = updated_at
WHERE due_at IS NULL;

-- Backfill resolved time for tickets already resolved: the last update time is the best value we have.
UPDATE tickets
SET resolved_at = updated_at,
    updated_at = updated_at
WHERE status = 'RESOLVED' AND resolved_at IS NULL;