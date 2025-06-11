--liquibase formatted sql

--changeset TymofiiSkrypko:add-oauth2-support-to-user-table
ALTER TABLE users
    ADD COLUMN provider VARCHAR(30),
    ADD COLUMN provider_id VARCHAR(255);

--changeset TymofiiSkrypko:remove-username-unique-constraint
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_username_key;
