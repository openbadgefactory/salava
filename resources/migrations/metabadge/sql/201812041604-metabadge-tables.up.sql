CREATE TABLE `factory_metabadge` (
  `id` varchar(255) NOT NULL,
  `name` text,
  `description` text,
  `criteria` mediumtext,
  `image_file` varchar(255) DEFAULT NULL,
  `min_required` bigint(20) unsigned DEFAULT 0,
  `factory_url` varchar(500) NOT NULL,
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
  `user_badge_id` bigint(20) unsigned PRIMARY KEY NOT NULL,
  `meta_badge` varchar(255) DEFAULT NULL,
  `meta_badge_req` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
