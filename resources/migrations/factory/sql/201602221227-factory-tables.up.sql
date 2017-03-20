CREATE TABLE `pending_factory_badge` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `assertion_url` varchar(500) NOT NULL,
  `email` varchar(255) NOT NULL,
  `ctime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;