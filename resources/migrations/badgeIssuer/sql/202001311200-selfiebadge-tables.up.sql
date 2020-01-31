CREATE TABLE IF NOT EXISTS `selfie_badge` (
  `id` varchar(255) NOT NULL,
  `creator_id`  bigint(20) unsigned NOT NULL,
  `name` text,
  `description` text,
  `criteria` mediumtext,
  `image` varchar(255) DEFAULT NULL,
  `tags` text,
  `issuable_from_gallery` boolean DEFAULT 0,
  `deleted` boolean DEFAULT 0 ,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;
