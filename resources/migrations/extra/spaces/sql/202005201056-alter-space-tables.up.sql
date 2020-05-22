ALTER TABLE `space` ADD COLUMN `alias` varchar(255) DEFAULT NULL AFTER `name`,
  ADD COLUMN `valid_until` bigint(20) unsigned DEFAULT NULL AFTER `visibility`;

--;;

ALTER TABLE `user_space`
  DROP COLUMN id,
  ADD PRIMARY KEY (user_id, space_id);
--;;
