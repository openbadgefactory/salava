CREATE TABLE `badge_advert` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `remote_url` varchar(500) NOT NULL,
  `remote_id` varchar(255) NOT NULL,
  `remote_issuer_id` varchar(255) NOT NULL,
  `info` text,
  `application_url` varchar(500) NOT NULL,
  `issuer_content_id` varchar(255) NOT NULL,
  `badge_content_id` varchar(255) NOT NULL,
  `criteria_content_id` varchar(255) NOT NULL,
  `kind` enum('application', 'advert') DEFAULT 'advert',
  `country` varchar(255) NOT NULL,
  `not_before` bigint(20) DEFAULT NULL,
  `not_after` bigint(20) DEFAULT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  `deleted` tinyint unsigned DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;

