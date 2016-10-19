CREATE TABLE `social_connections_badge` (
  `user_id` bigint(20) unsigned NOT NULL,
  `badge_content_id` varchar(255) NOT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`badge_content_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
