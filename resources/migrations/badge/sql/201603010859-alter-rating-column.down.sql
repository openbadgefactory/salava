UPDATE `badge` SET rating = rating / 10;
--;;
ALTER TABLE `badge` MODIFY `rating` decimal(2,1) DEFAULT NULL;