CREATE TABLE `creator_content` (
  `id` varchar(255) NOT NULL,
  `url` varchar(500) DEFAULT NULL,
  `name` text,
  `description` text,
  `image_file` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `json_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;