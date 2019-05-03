CREATE TABLE `gallery` (
    `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `badge_name`  varchar(255) NOT NULL,
    `badge_image` varchar(255) NOT NULL,
    `issuer_name` varchar(255) NOT NULL,
    `badge_id`    varchar(255) NOT NULL,
    UNIQUE INDEX gallery_badge_idx (`badge_name`,`badge_image`,`issuer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
