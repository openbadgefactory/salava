ALTER TABLE user ADD COLUMN location_public tinyint UNSIGNED DEFAULT 0 AFTER country;

--;;

ALTER TABLE user ADD COLUMN location_lng float DEFAULT NULL AFTER country;

--;;

ALTER TABLE user ADD COLUMN location_lat float DEFAULT NULL AFTER country;

--;;

ALTER TABLE user_badge ADD COLUMN location_lng float DEFAULT NULL AFTER rating;

--;;

ALTER TABLE user_badge ADD COLUMN location_lat float DEFAULT NULL AFTER rating;

--;;
