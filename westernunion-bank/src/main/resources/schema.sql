-- =========================================================
-- Western Union Bank - MySQL Schema
-- (Reference only: Hibernate ddl-auto=update will create/
--  update these tables automatically on startup. Run this
--  manually only if you prefer to provision the schema
--  yourself, e.g. with ddl-auto=validate or none.)
-- =========================================================

CREATE DATABASE IF NOT EXISTS westernunion_bank
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE westernunion_bank;

CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name    VARCHAR(100)  NOT NULL,
    email        VARCHAR(150)  NOT NULL UNIQUE,
    phone        VARCHAR(20)   NOT NULL UNIQUE,
    password     VARCHAR(255)  NOT NULL,
    created_at   DATETIME      NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS accounts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number  VARCHAR(20)    NOT NULL UNIQUE,
    user_id         BIGINT         NOT NULL UNIQUE,
    balance         DECIMAL(19,2)  NOT NULL DEFAULT 0.00,
    account_type    VARCHAR(20)    NOT NULL DEFAULT 'SAVINGS',
    status          VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME       NOT NULL,
    version         BIGINT         DEFAULT 0,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_id         VARCHAR(40)    NOT NULL UNIQUE,
    type                 VARCHAR(20)    NOT NULL,
    from_account_number  VARCHAR(20)    NOT NULL,
    to_account_number    VARCHAR(20),
    amount               DECIMAL(19,2)  NOT NULL,
    balance_after        DECIMAL(19,2)  NOT NULL,
    description          VARCHAR(255),
    status               VARCHAR(20)    NOT NULL DEFAULT 'SUCCESS',
    timestamp            DATETIME       NOT NULL,
    INDEX idx_from_account (from_account_number),
    INDEX idx_to_account (to_account_number)
) ENGINE=InnoDB;
