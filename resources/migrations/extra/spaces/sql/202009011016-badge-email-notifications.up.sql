ALTER TABLE `user_badge` ADD COLUMN email_notifications tinyint UNSIGNED DEFAULT 0 AFTER location_lng;

--;;

CREATE TABLE `notifications` (
  id bigint UNSIGNED PRIMARY KEY NOT NULL AUTO_INCREMENT,
  space_id bigint UNSIGNED DEFAULT NULL,
  message mediumtext,
  ctime bigint UNSIGNED NOT NULL,
  sent_by  bigint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
