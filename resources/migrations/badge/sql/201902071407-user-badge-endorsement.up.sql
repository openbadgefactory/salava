CREATE TABLE user_badge_endorsement (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_badge_id` bigint(20) UNSIGNED NOT NULL,
  `endorser_id` bigint(20) UNSIGNED DEFAULT NULL,
  `content` text,
  `status` enum('pending','accepted','declined') DEFAULT 'pending',
  `ctime` bigint UNSIGNED DEFAULT NULL,
  `mtime` bigint UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
