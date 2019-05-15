CREATE TABLE `page_block_profile_fields` (
  `page_id` bigint(20) unsigned NOT NULL,
  `field` varchar(255) NOT NULL,
  PRIMARY KEY (`page_id`, `field`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
