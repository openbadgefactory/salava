CREATE TABLE `badge_message` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `badge_content_id` varchar(255) NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `message` text NOT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  `deleted` boolean DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
