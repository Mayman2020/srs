SET search_path TO srs_system, public;

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS profile_image_path varchar(512),
    ADD COLUMN IF NOT EXISTS profile_image_content_type varchar(128);
