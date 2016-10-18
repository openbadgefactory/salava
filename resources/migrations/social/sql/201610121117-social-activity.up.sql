CREATE TABLE `social_event` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `subject` bigint(20) unsigned NOT NULL,
  `verb` enum ('message', 'follow', 'publish', 'delete_message') DEFAULT 'message',
  `object` varchar(255) NOT NULL,
  `type` enum ('badge', 'user', 'page') DEFAULT 'badge',
  `ctime` bigint(20) unsigned NOT NULL,
  `mtime` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
--;;
CREATE TABLE `social_event_owners` (
  `owner` bigint(20) unsigned NOT NULL,  
  `event_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`owner`, `event_id` )
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
