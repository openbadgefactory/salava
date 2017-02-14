CREATE TABLE `social_connections_badge_advert` (
  `user_id` bigint(20) unsigned NOT NULL,
  `badge_advert_id` bigint(20) NOT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`badge_advert_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
