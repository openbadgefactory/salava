CREATE TABLE `page_block_showcase` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `page_id` bigint(20) unsigned NOT NULL,
  `title` text,
  `format` enum('short','long') DEFAULT NULL,
  `block_order` tinyint(3) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `page_block_showcase_has_badge` (
  `block_id` bigint(20) unsigned NOT NULL,
  `badge_id` bigint(20) unsigned NOT NULL,
  `badge_order` tinyint(3) unsigned NOT NULL,
  PRIMARY KEY (`block_id`,`badge_id`,`badge_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
