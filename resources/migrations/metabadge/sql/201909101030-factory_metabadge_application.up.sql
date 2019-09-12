ALTER TABLE `factory_metabadge_required` ADD COLUMN `application_url`  varchar(500) DEFAULT NULL AFTER `image_file`;

--;;

ALTER TABLE `factory_metabadge_required` ADD COLUMN `not_before` bigint(20) DEFAULT NULL AFTER `application_url`;

--;;

ALTER TABLE `factory_metabadge_required` ADD COLUMN  `not_after` bigint(20) DEFAULT NULL AFTER `not_before`;
