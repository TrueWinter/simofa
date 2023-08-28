SET DEFAULT_STORAGE_ENGINE = INNODB;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE IF NOT EXISTS `users` (
    `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` varchar(20) NOT NULL,
    `password` char(60) NOT NULL
) CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `deployment_servers` (
    `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` varchar(20) NOT NULL,
    `url` varchar(256) NOT NULL,
    `key` varchar(60) NOT NULL
) CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `git` (
    `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` varchar(40) NOT NULL,
    `password` char(80) NOT NULL
) CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `websites` (
    `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` varchar(40) NOT NULL,
    `docker_image` varchar(40) NOT NULL,
    `memory` int(6) NOT NULL,
    `cpu` decimal(4,2) NOT NULL,
    `git_url` varchar(255) NOT NULL,
    `git_branch` varchar(40) NOT NULL,
    `git_credential` int,
    -- The command run to build the site
    `build_command` varchar(255) NOT NULL,
    -- The command run on the deployment server.
    -- Use this to unzip the site, delete the old one,
    -- and move the new one to the web server directory.
    `deployment_command` varchar(255) NOT NULL,
    -- The command run on the deployment server if
    -- the deployment command fails. Use this to clean
    -- up files and move the original site back into place.
    `deployment_failed_command` varchar(255) NOT NULL,
    `deployment_server` int NOT NULL,
    `deploy_token` VARCHAR(36) NOT NULL,
    FOREIGN KEY (`deployment_server`) REFERENCES `deployment_servers` (`id`),
    FOREIGN KEY (`git_credential`) REFERENCES `git` (`id`)
) CHARACTER SET utf8 COLLATE utf8_unicode_ci;

CREATE TABLE IF NOT EXISTS `templates` (
    `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` varchar(40) NOT NULL,
    `template` varchar(4000) NOT NULL
) CHARACTER SET utf8 COLLATE utf8_unicode_ci;