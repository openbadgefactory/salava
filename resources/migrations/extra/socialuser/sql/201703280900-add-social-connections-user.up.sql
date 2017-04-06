CREATE TABLE `social_connections_user` (
  `owner_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `status` enum ('accepted', 'pending', 'declined') DEFAULT 'pending',
  `ctime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`owner_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
