--liquibase formatted sql

--changeset TymofiiSkrypko:create-user-table
CREATE TABLE users
(
    id             UUID PRIMARY KEY,
    username       VARCHAR(32) NOT NULL UNIQUE,
    password       VARCHAR(128) NOT NULL,
    email          VARCHAR(128) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

--changeset TymofiiSkrypko:create-roles-table
CREATE TABLE roles
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(64) NOT NULL UNIQUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by     UUID NOT NULL,
    modified_by    UUID NOT NULL
);

--changeset TymofiiSkrypko:create-user_roles-table
CREATE TABLE user_roles
(
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id),
    role_id        UUID NOT NULL REFERENCES roles(id),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by     UUID NOT NULL,
    modified_by    UUID NOT NULL
);

--changeset TymofiiSkrypko:create-permissions-table
CREATE TABLE permissions
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(64) NOT NULL UNIQUE,
    description    VARCHAR(256),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by     UUID NOT NULL,
    modified_by    UUID NOT NULL
);

--changeset TymofiiSkrypko:create-role_permissions-table
CREATE TABLE role_permissions
(
    id               UUID PRIMARY KEY,
    role_id          UUID NOT NULL REFERENCES roles(id),
    permission_id    UUID NOT NULL REFERENCES permissions(id),
    level            VARCHAR(128) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    modified_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       UUID NOT NULL,
    modified_by      UUID NOT NULL
);
