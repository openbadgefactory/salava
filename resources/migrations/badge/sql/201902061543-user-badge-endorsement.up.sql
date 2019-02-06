CREATE TABLE user_badge_endorsement (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_badge_id` bigint(20) UNSIGNED NOT NULL
  `endorser_id` bigint(20) UNSIGNED DEFAULT NULL,
  `endorser_firstname` VARCHAR(255) NOT NULL,
  `endorser_lastname` VARCHAR(255) NOT NULL,
  `endorser_email` VARCHAR(255) NOT NULL,
  `content` text,
  `ctime` bigint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
