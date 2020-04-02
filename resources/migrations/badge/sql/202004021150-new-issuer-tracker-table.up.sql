CREATE TABLE IF NOT EXISTS `new_issuer_history` (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `issuer_content_id` varchar(255) NOT NULL,
  `issuer_name` text,
  `ctime` bigint(20) UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TRIGGER `new_issuer` BEFORE INSERT ON `issuer_content` FOR EACH ROW
  INSERT INTO `new_issuer_history` (issuer_content_id, issuer_name, ctime) VALUES (NEW.id, NEW.name, UNIX_TIMESTAMP())
   WHERE NEW.name NOT IN (SELECT DISTINCT name FROM issuer_content);
