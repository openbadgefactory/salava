CREATE TABLE `endorsement_content` (
  `id` varchar(255) NOT NULL,
  `issuer_content_id` varchar(255) NOT NULL,
  `content` text,
  `issued_on` bigint UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE `badge_endorsement_content` (
    `badge_id` varchar(255) NOT NULL,
    `endorsement_content_id` varchar(255) NOT NULL,
    PRIMARY KEY (`badge_id`, `endorsement_content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--;;

CREATE TABLE issuer_endorsement_content (
    `issuer_content_id` varchar(255) NOT NULL,
    `endorsement_content_id` varchar(255) NOT NULL,
    PRIMARY KEY (`issuer_content_id`, `endorsement_content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
