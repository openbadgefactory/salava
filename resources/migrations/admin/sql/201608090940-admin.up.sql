CREATE TABLE `report_ticket` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `item_id`  bigint(20) unsigned DEFAULT NULL,
  `item_name` varchar(255) NOT NULL,
  `item_type` varchar(255) NOT NULL,
  `item_url` varchar(255) DEFAULT NULL,
  `item_content_id` varchar(255) DEFAULT NULL,
  `reporter_id`  bigint(20) unsigned NOT NULL,
  `report_type` enum ('inappropriate' 'bug' 'mistranslation' 'other' 'fakebadge') DEFAULT 'bug',
  `status` enum('open','closed') DEFAULT 'open',
  `description` varchar(255) NOT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
