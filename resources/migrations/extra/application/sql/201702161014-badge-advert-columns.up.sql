ALTER TABLE badge_advert
ADD COLUMN criteria_url varchar(500) NOT NULL AFTER criteria_content_id,
ADD COLUMN application_url_label varchar(255) DEFAULT NULL AFTER application_url;
