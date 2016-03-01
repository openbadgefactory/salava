ALTER TABLE `badge` MODIFY `rating` smallint(1) DEFAULT NULL;
--;;
UPDATE `badge` SET rating = rating * 10;