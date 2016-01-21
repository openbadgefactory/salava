CREATE TABLE `user` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  `last_login` bigint(20) unsigned,
  `pass` varchar(255) DEFAULT NULL,
  `language` varchar(255) DEFAULT 'en',
  `country` varchar(255),
  `profile_visibility` enum('internal','public') DEFAULT 'internal',
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  `activation_code` varchar(255),
  `activated` boolean NOT NULL DEFAULT 0,
  `deleted` boolean DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `user_email` (
  `user_id` bigint(20) unsigned NOT NULL,
  `email` varchar(255) NOT NULL,
  `verified` boolean DEFAULT 0,
  `verification_key` varchar(255) DEFAULT NULL,
  `primary_address` boolean DEFAULT 0,
  `backpack_id` bigint(20) unsigned DEFAULT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`user_id`,`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `user_profile` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `field` text,
  `value` text,
  `field_order` tinyint(3) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
