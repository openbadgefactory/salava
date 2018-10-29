ALTER TABLE badge_advert
ADD COLUMN remote_issuer_banner VARCHAR(255) DEFAULT NULL AFTER remote_issuer_id,
ADD COLUMN remote_issuer_tier enum('free','basic', 'premium', 'pro') DEFAULT NULL AFTER remote_issuer_id;

