-- 공통 옵션
SET NAMES utf8mb4;
SET time_zone = '+09:00';

CREATE TABLE IF NOT EXISTS provider (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        provider_type VARCHAR(50) NOT NULL,        -- AWS / VSPHERE 등
    name VARCHAR(200) NOT NULL,                -- 표시명
    account_id VARCHAR(200) NULL,              -- 실제 계정/프로젝트 식별자
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_provider_type (provider_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS os_image (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        provider_id BIGINT NOT NULL,
                                        provider_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    image_id VARCHAR(200) NOT NULL,            -- AMI/템플릿 ID
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_osimg_provider (provider_id, provider_type),
    CONSTRAINT fk_osimg_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS instance_type (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             provider_id BIGINT NOT NULL,
                                             provider_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    vcpu INT NOT NULL,
    memory_mb INT NOT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_it_provider (provider_id, provider_type),
    CONSTRAINT fk_it_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS datastore (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         provider_id BIGINT NOT NULL,
                                         provider_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    capacity_gb INT NULL,
    free_gb INT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ds_provider (provider_id, provider_type),
    CONSTRAINT fk_ds_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS network (
                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                       provider_id BIGINT NOT NULL,
                                       provider_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    cidr VARCHAR(50) NULL,
    vlan_id INT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_net_provider (provider_id, provider_type),
    CONSTRAINT fk_net_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS security_profile (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                provider_id BIGINT NOT NULL,
                                                provider_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    rules JSON NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sp_provider (provider_id, provider_type),
    CONSTRAINT fk_sp_provider FOREIGN KEY (provider_id) REFERENCES provider(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS terraform_module (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                provider_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    source VARCHAR(500) NOT NULL,              -- registry/source
    version VARCHAR(50) NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tfmod_provider (provider_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS terraform_variable (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                  module_id BIGINT NOT NULL,
                                                  `key` VARCHAR(200) NOT NULL,
    `type` VARCHAR(100) NULL,
    default_value TEXT NULL,
    description TEXT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_tfvar_module (module_id),
    CONSTRAINT fk_tfvar_module FOREIGN KEY (module_id) REFERENCES terraform_module(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ansible_playbook (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                name VARCHAR(200) NOT NULL,
    path VARCHAR(500) NOT NULL,                -- repo 내 경로
    tags VARCHAR(500) NULL,
    description TEXT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS installable_package (
                                                   id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                   provider_type VARCHAR(50) NULL,            -- 특정 프로바이더만 제공 시
    name VARCHAR(200) NOT NULL,
    version VARCHAR(100) NULL,
    description TEXT NULL,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pkg_provider (provider_type)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
