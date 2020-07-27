ALTER TABLE `oauth2_token` ADD COLUMN `firebase_token` varchar(500) DEFAULT NULL AFTER `user_id`;
