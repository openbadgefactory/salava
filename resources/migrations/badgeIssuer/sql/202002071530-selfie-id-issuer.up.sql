ALTER TABLE user_badge ADD COLUMN selfie_id varchar(255) DEFAULT NULL AFTER gallery_id;

--;;

ALTER TABLE user_badge ADD COLUMN issuer_id bigint(20) UNSIGNED DEFAULT NULL AFTER selfie_id;
