--name: create-request-endorsement-table!
--create user_badge_endorsement_request table
CREATE TABLE IF NOT EXISTS user_badge_endorsement_request(
    `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_badge_id`  bigint(20) NOT NULL,
    `status` enum('pending','endorsed','declined') DEFAULT 'pending',
    `content` text,
    `issuer_name` text,
    `issuer_id` bigint(20) UNSIGNED DEFAULT NULL,
    `issuer_url` varchar(500) DEFAULT NULL,
    `ctime` bigint UNSIGNED DEFAULT NULL,
    `mtime` bigint UNSIGNED DEFAULT NULL
);

--name: drop-request-endorsement-table!
--drop user_badge_endorsement_request table
DROP TABLE user_badge_endorsement_request
