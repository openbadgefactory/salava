ALTER TABLE `badge_tag` DROP COLUMN `id`;
--;;
ALTER TABLE `badge_tag` ADD PRIMARY KEY (`badge_id`,`tag`);