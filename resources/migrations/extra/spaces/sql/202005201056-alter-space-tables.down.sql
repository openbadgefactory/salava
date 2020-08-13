ALTER TABLE `space`
  DROP COLUMN `alias`,
  DROP COLUMN `valid_until`;

--;;

ALTER TABLE `user_space`
  DROP PRIMARY KEY,
  ADD COLUMN id bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY;
  
--;;
