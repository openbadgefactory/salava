CREATE TABLE IF NOT EXISTS `system_properties`(
  `name` varchar(255) NOT NULL,
  `value` MEDIUMTEXT,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;
