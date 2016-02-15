ALTER TABLE `user` ADD COLUMN `profile_picture` varchar(255) DEFAULT NULL AFTER `profile_visibility`,
                   ADD COLUMN `about` text DEFAULT NULL AFTER `profile_picture`;