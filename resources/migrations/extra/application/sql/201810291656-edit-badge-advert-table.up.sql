ALTER TABLE badge_advert
ADD COLUMN issuer_banner VARCHAR(255) DEFAULT NULL AFTER issuer_content_id,
ADD COLUMN `issuer_tier` enum('free','basic', 'premium', 'pro') DEFAULT NULL AFTER issuer_content_id;

