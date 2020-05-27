CREATE TABLE user_badge_endorsement_ext (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `external_id` varchar(255) NOT NULL,
  `user_badge_id` bigint(20) UNSIGNED NOT NULL,
  `issuer_id` varchar(255) NOT NULL,
  `issuer_name` text,
  `issuer_url` varchar(500) DEFAULT NULL,
  `content` text,
  `status` enum('pending','accepted','declined') DEFAULT 'pending',
  `ctime` bigint UNSIGNED DEFAULT NULL,
  `mtime` bigint UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
