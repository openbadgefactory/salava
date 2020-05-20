CREATE TABLE `oauth2_token` (
  `id` bigint(20) unsigned NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `client_id` varchar(255) NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `scope` varchar(255) DEFAULT NULL,
  `auth_code` varchar(255) DEFAULT NULL,
  `auth_code_challenge` varchar(255) DEFAULT NULL,
  `refresh_token` varchar(255) DEFAULT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  INDEX (`client_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
