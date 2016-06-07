ALTER TABLE `badge_content_tag` DROP COLUMN `id`;
--;;
ALTER TABLE `badge_content_tag` ADD PRIMARY KEY (`badge_content_id`,`tag`);