-- =============================================================================
-- V6__indexes.sql
-- Search & FK performance indexes, partial uniques (soft-delete safe),
-- audit column foreign keys → app_user, and updated_at triggers.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Partial UNIQUE: lookup & master "code" columns (active rows only)
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX ux_correspondence_type_code_active
  ON correspondence_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_status_global_code_active
  ON correspondence_status (code)
  WHERE correspondence_type_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_status_type_code_active
  ON correspondence_status (correspondence_type_id, code)
  WHERE correspondence_type_id IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_priority_code_active
  ON priority (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_confidentiality_code_active
  ON confidentiality (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_workflow_action_type_code_active
  ON workflow_action_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_notification_channel_code_active
  ON notification_channel (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_notification_event_type_code_active
  ON notification_event_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_attachment_content_type_code_active
  ON attachment_content_type (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_workflow_instance_status_code_active
  ON workflow_instance_status (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_notification_delivery_status_code_active
  ON notification_delivery_status (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_department_code_active
  ON department (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_organization_code_active
  ON organization (code) WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_classification_root_code_active
  ON classification (code) WHERE parent_id IS NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_classification_child_code_active
  ON classification (parent_id, code) WHERE parent_id IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX ux_permission_code_active
  ON permission (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_role_code_active
  ON role (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_template_code_active
  ON template (code) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_app_user_username_active
  ON app_user (username) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_app_user_email_active
  ON app_user (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_reference_active
  ON correspondence (reference_number) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_correspondence_barcode_active
  ON correspondence (barcode_value) WHERE deleted_at IS NULL AND barcode_value IS NOT NULL;

CREATE UNIQUE INDEX ux_workflow_instance_correspondence_active
  ON workflow_instance (correspondence_id) WHERE deleted_at IS NULL;

-- BR-014: at most one non-revoked, non-deleted delegation row per delegator
CREATE UNIQUE INDEX ux_delegation_one_active_per_delegator
  ON delegation (delegator_user_id)
  WHERE NOT is_revoked AND deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Foreign keys: audit & soft-delete actor columns → app_user
-- -----------------------------------------------------------------------------
ALTER TABLE correspondence_type
  ADD CONSTRAINT fk_correspondence_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_status
  ADD CONSTRAINT fk_correspondence_status_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_status_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_status_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE priority
  ADD CONSTRAINT fk_priority_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_priority_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_priority_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE confidentiality
  ADD CONSTRAINT fk_confidentiality_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_confidentiality_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_confidentiality_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_action_type
  ADD CONSTRAINT fk_workflow_action_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_channel
  ADD CONSTRAINT fk_notification_channel_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_channel_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_channel_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_event_type
  ADD CONSTRAINT fk_notification_event_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_event_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_event_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE attachment_content_type
  ADD CONSTRAINT fk_attachment_content_type_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_content_type_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_content_type_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_instance_status
  ADD CONSTRAINT fk_workflow_instance_status_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_status_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_status_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_delivery_status
  ADD CONSTRAINT fk_notification_delivery_status_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_status_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_status_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE department
  ADD CONSTRAINT fk_department_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_department_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_department_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE organization
  ADD CONSTRAINT fk_organization_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_organization_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_organization_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE classification
  ADD CONSTRAINT fk_classification_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_classification_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_classification_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE permission
  ADD CONSTRAINT fk_permission_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_permission_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_permission_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE role
  ADD CONSTRAINT fk_role_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_role_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_role_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE role_permission
  ADD CONSTRAINT fk_role_permission_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_role_permission_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE app_user
  ADD CONSTRAINT fk_app_user_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_app_user_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_app_user_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE user_role
  ADD CONSTRAINT fk_user_role_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_user_role_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence
  ADD CONSTRAINT fk_correspondence_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_recipient
  ADD CONSTRAINT fk_correspondence_recipient_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_recipient_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_recipient_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE attachment
  ADD CONSTRAINT fk_attachment_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE attachment_version
  ADD CONSTRAINT fk_attachment_version_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_version_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_attachment_version_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE correspondence_comment
  ADD CONSTRAINT fk_correspondence_comment_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_comment_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_correspondence_comment_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE delegation
  ADD CONSTRAINT fk_delegation_revoked_by FOREIGN KEY (revoked_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_delegation_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_delegation_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_delegation_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE template
  ADD CONSTRAINT fk_template_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_template_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_template_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE retention_policy
  ADD CONSTRAINT fk_retention_policy_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_retention_policy_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_retention_policy_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE audit_log
  ADD CONSTRAINT fk_audit_log_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_audit_log_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_instance
  ADD CONSTRAINT fk_workflow_instance_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_instance_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE workflow_action
  ADD CONSTRAINT fk_workflow_action_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_workflow_action_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification
  ADD CONSTRAINT fk_notification_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

ALTER TABLE notification_delivery
  ADD CONSTRAINT fk_notification_delivery_created_by FOREIGN KEY (created_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notification_delivery_deleted_by FOREIGN KEY (deleted_by) REFERENCES app_user (id) ON DELETE SET NULL;

-- -----------------------------------------------------------------------------
-- Secondary indexes (queries, joins, reporting)
-- -----------------------------------------------------------------------------
CREATE INDEX ix_department_parent ON department (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_organization_parent ON organization (parent_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_classification_parent ON classification (parent_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_correspondence_type ON correspondence (correspondence_type_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_status ON correspondence (correspondence_status_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_priority ON correspondence (priority_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_confidentiality ON correspondence (confidentiality_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_classification ON correspondence (classification_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_owner_dept ON correspondence (owner_department_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_sender_org ON correspondence (sender_organization_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_recipient_org ON correspondence (recipient_organization_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_created_at ON correspondence (created_at) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_due_date ON correspondence (due_date) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_created_by ON correspondence (created_by) WHERE deleted_at IS NULL;

CREATE INDEX ix_correspondence_recipient_dept ON correspondence_recipient (department_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_correspondence_recipient_corr ON correspondence_recipient (correspondence_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_attachment_correspondence ON attachment (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_attachment_content_type ON attachment (content_type_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_attachment_version_attachment ON attachment_version (attachment_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_comment_correspondence ON correspondence_comment (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_comment_author ON correspondence_comment (author_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_comment_parent ON correspondence_comment (parent_comment_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_delegation_delegator ON delegation (delegator_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_delegation_delegate ON delegation (delegate_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_delegation_window ON delegation (start_at, end_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_template_type ON template (correspondence_type_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_retention_classification ON retention_policy (classification_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_retention_corr_type ON retention_policy (correspondence_type_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_audit_occurred ON audit_log (occurred_at);
CREATE INDEX ix_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX ix_audit_actor ON audit_log (actor_user_id);

CREATE INDEX ix_workflow_instance_corr ON workflow_instance (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_instance_status ON workflow_instance (workflow_instance_status_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_instance_started ON workflow_instance (started_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_workflow_action_instance ON workflow_action (workflow_instance_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_corr ON workflow_action (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_type ON workflow_action (workflow_action_type_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_actor ON workflow_action (actor_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_workflow_action_created ON workflow_action (created_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_notification_recipient ON notification (recipient_user_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_event ON notification (notification_event_type_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_corr ON notification (correspondence_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_unread ON notification (recipient_user_id) WHERE read_at IS NULL AND deleted_at IS NULL;
CREATE INDEX ix_notification_created ON notification (created_at) WHERE deleted_at IS NULL;

CREATE INDEX ix_notification_delivery_notification ON notification_delivery (notification_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_delivery_channel ON notification_delivery (notification_channel_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_notification_delivery_status ON notification_delivery (notification_delivery_status_id) WHERE deleted_at IS NULL;

CREATE INDEX ix_app_user_department ON app_user (department_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- BEFORE UPDATE triggers: maintain updated_at via ac_set_updated_at()
-- -----------------------------------------------------------------------------
CREATE TRIGGER tr_correspondence_type_updated_at BEFORE UPDATE ON correspondence_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_correspondence_status_updated_at BEFORE UPDATE ON correspondence_status FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_priority_updated_at BEFORE UPDATE ON priority FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_confidentiality_updated_at BEFORE UPDATE ON confidentiality FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_workflow_action_type_updated_at BEFORE UPDATE ON workflow_action_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_channel_updated_at BEFORE UPDATE ON notification_channel FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_event_type_updated_at BEFORE UPDATE ON notification_event_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_attachment_content_type_updated_at BEFORE UPDATE ON attachment_content_type FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_workflow_instance_status_updated_at BEFORE UPDATE ON workflow_instance_status FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_delivery_status_updated_at BEFORE UPDATE ON notification_delivery_status FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_department_updated_at BEFORE UPDATE ON department FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_organization_updated_at BEFORE UPDATE ON organization FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_classification_updated_at BEFORE UPDATE ON classification FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_permission_updated_at BEFORE UPDATE ON permission FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_role_updated_at BEFORE UPDATE ON role FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_role_permission_updated_at BEFORE UPDATE ON role_permission FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_app_user_updated_at BEFORE UPDATE ON app_user FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_user_role_updated_at BEFORE UPDATE ON user_role FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_correspondence_updated_at BEFORE UPDATE ON correspondence FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_correspondence_recipient_updated_at BEFORE UPDATE ON correspondence_recipient FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_attachment_updated_at BEFORE UPDATE ON attachment FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_attachment_version_updated_at BEFORE UPDATE ON attachment_version FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_correspondence_comment_updated_at BEFORE UPDATE ON correspondence_comment FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_delegation_updated_at BEFORE UPDATE ON delegation FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_template_updated_at BEFORE UPDATE ON template FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_retention_policy_updated_at BEFORE UPDATE ON retention_policy FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_audit_log_updated_at BEFORE UPDATE ON audit_log FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_workflow_instance_updated_at BEFORE UPDATE ON workflow_instance FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_workflow_action_updated_at BEFORE UPDATE ON workflow_action FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();

CREATE TRIGGER tr_notification_updated_at BEFORE UPDATE ON notification FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
CREATE TRIGGER tr_notification_delivery_updated_at BEFORE UPDATE ON notification_delivery FOR EACH ROW EXECUTE PROCEDURE ac_set_updated_at();
