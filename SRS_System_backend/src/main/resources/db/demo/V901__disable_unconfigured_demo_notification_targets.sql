-- Demo endpoints must never dispatch until explicitly configured by a developer.
-- Real targets created through the admin UI are unaffected.
UPDATE notification_channel_target
SET enabled = FALSE,
    updated_at = NOW()
WHERE target_code = 'demo-corp-webhook'
  AND target_url LIKE '%.invalid/%'
  AND deleted_at IS NULL;
