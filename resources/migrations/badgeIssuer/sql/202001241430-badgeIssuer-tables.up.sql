CREATE TABLE `selfie_badge`(
  `id` varchar(255) NOT NULL,
  `creator_id`  bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(500) NOT NULL,
  `image` longtext NOT NULL,
  `issuable_from_gallery` boolean DEFAULT 0,
  `deleted` boolean DEFAULT 0 ,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;
