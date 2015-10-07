CREATE TABLE `badge` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `assertion_url` varchar(500) DEFAULT NULL,
  `assertion_jws` text,
  `assertion_json` text,
  `badge_url` varchar(500) DEFAULT NULL,
  `issuer_url` varchar(500) DEFAULT NULL,
  `creator_url` varchar(500) DEFAULT NULL,
  `criteria_url` varchar(500) DEFAULT NULL,
  `badge_content_id` varchar(255) DEFAULT NULL,
  `issuer_content_id` varchar(255) DEFAULT NULL,
  `creator_content_id` varchar(255) DEFAULT NULL,
  `issued_on` bigint(20) unsigned DEFAULT NULL,
  `expires_on` bigint(20) unsigned DEFAULT NULL,
  `evidence_url` varchar(500) DEFAULT NULL,
  `status` enum('pending','accepted','declined') DEFAULT 'pending',
  `visibility` enum('private','internal','public') DEFAULT 'private',
  `show_recipient_name` boolean DEFAULT 0,
  `rating` tinyint(3) unsigned DEFAULT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  `deleted` boolean DEFAULT 0,
  `revoked` boolean DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
CREATE TABLE `badge_content` (
  `id` varchar(255) NOT NULL,
  `name` text,
  `description` text,
  `image_file` varchar(255) DEFAULT NULL,
  `criteria_markdown` mediumtext,
  `criteria_html` mediumtext,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
CREATE TABLE `badge_content_alignment` (
  `badge_content_id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `url` varchar(500) NOT NULL,
  `description` text,
  PRIMARY KEY (`badge_content_id`,`name`,`url`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
CREATE TABLE `badge_content_tag` (
  `badge_content_id` varchar(255) NOT NULL,
  `tag` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`badge_content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
CREATE TABLE `badge_tag` (
  `badge_id` bigint(20) unsigned NOT NULL,
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`badge_id`,`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
CREATE TABLE `issuer_content` (
  `id` varchar(255) NOT NULL,
  `name` text,
  `url` varchar(500) DEFAULT NULL,
  `description` text,
  `image_file` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `revocation_list_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
