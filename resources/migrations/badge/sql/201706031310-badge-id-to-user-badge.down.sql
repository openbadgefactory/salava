ALTER TABLE `badge_view` CHANGE COLUMN `user_badge_id` badge_id bigint(20);
--;;

ALTER TABLE `badge_tag` CHANGE COLUMN `user_badge_id` badge_id bigint(20);

--;;

ALTER TABLE `badge_congratulation` CHANGE COLUMN `user_badge_id` badge_id bigint(20);
