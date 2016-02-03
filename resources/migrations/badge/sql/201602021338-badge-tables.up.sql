CREATE TABLE `badge_congratulation` (
  `badge_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`badge_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;