CREATE TABLE `space` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `logo` varchar(255) DEFAULT NULL,
  `banner` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT 'active',
  `visibility` varchar (255) DEFAULT 'closed',
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  `last_modified_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `user_space`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `space_id` bigint(20) unsigned NOT NULL,
  `role` varchar(255) DEFAULT 'member',
  `default_space` boolean DEFAULT 0,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `space_properties`(
  `space_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`space_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `space_admin_pending`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `space_id`  bigint(20) unsigned NOT NULL,
  `email` varchar(255) NOT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
