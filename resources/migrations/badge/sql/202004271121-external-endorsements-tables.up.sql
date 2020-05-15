CREATE TABLE IF NOT EXISTS `user_badge_endorsement_request_ext` (
    `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_badge_id`  bigint(20) NOT NULL,
    `status` enum('pending','endorsed','declined') DEFAULT 'pending',
    `content` text,
    `issuer_email` varchar(255) NOT NULL,
    `ctime` bigint UNSIGNED DEFAULT NULL,
    `mtime` bigint UNSIGNED DEFAULT NULL
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE IF NOT EXISTS `user_ext` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `ext_id` varchar(255) NOT NULL,
  `url` varchar(500) DEFAULT NULL,
  `name` text,
  `description` text,
  `image_file` varchar(255) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `ctime` bigint UNSIGNED DEFAULT NULL,
  `mtime` bigint UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`, `ext_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
