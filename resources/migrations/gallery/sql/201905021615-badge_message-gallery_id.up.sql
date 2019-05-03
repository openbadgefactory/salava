ALTER TABLE `badge_message` ADD COLUMN `gallery_id` bigint unsigned DEFAULT NULL AFTER badge_id;
--;;
ALTER TABLE `badge_message_view` ADD COLUMN `gallery_id` bigint unsigned DEFAULT NULL AFTER badge_id;
