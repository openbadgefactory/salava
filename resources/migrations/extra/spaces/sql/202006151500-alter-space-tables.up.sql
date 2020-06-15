ALTER TABLE `user_space` ADD COLUMN IF NOT EXISTS `status` VARCHAR(255) DEFAULT 'accepted' AFTER `role`;

--;;

DROP TABLE IF EXISTS `space_admin_pending`;

--;;

ALTER TABLE `space` MODIFY COLUMN `visibility` VARCHAR(255) NOT NULL DEFAULT 'private';
