CREATE TABLE `factory_metabadge` (
  `id` varchar(255) NOT NULL,
  `name` text,
  `description` text,
  `criteria` mediumtext,
  `image_file` varchar(255) DEFAULT NULL,
  `min_required` bigint(20) unsigned DEFAULT 0,
  `factory_url` varchar(500) DEFAULT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `factory_metabadge_required` (
  `metabadge_id` varchar(255) NOT NULL,
  `required_badge_id` varchar(255) NOT NULL,
  `name` text,
  `description` text,
  `criteria` mediumtext,
  `image_file` varchar(255) DEFAULT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`metabadge_id`, `required_badge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `user_badge_metabadge` (
  `user_badge_id` bigint(20) unsigned NOT NULL,
  `meta_badge` varchar(255) DEFAULT NULL,
  `meta_badge_req` varchar(255) DEFAULT NULL,
  `last_modified` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`user_badge_id`, `meta_badge`, `meta_badge_req`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `user_metabadge_received` (
  `metabadge_id` varchar(255) NOT NULL,
  `user_badge_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `name` text,
  `min_required` bigint(20) unsigned DEFAULT 0,
  `last_modified` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`user_badge_id`, `metabadge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `user_metabadge_required_received` (
  `metabadge_id` varchar(255) NOT NULL,
  `user_metabadge_id` varchar(255) NOT NULL,
  `user_required_badge_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `last_modified` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`metabadge_id`, `user_required_badge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
