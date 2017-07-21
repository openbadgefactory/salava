DROP TABLE IF EXISTS badge_content_tag_old;

--;;

RENAME TABLE badge_content_tag TO badge_content_tag_old;

--;;

CREATE TABLE `badge_content_tag` (
  `badge_content_id` varchar(255) NOT NULL,
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`badge_content_id`,`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

INSERT IGNORE INTO badge_content_tag (badge_content_id, tag)
       SELECT badge_content_id, tag FROM badge_content_tag_old;
