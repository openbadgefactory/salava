CREATE TABLE `badge_message_view` (
  `badge_content_id` varchar(255) NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`badge_content_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
