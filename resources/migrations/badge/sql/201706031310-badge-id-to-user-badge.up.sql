ALTER TABLE `badge_view` CHANGE COLUMN `badge_id` user_badge_id bigint(20);
--;;

ALTER TABLE `badge_tag` CHANGE COLUMN `badge_id` user_badge_id bigint(20);

--;;

ALTER TABLE `badge_congratulation` CHANGE COLUMN `badge_id` user_badge_id bigint(20);
