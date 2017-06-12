ALTER TABLE badge ADD COLUMN default_language_name varchar(255) AFTER default_language_code;

--;;

ALTER TABLE badge_content ADD COLUMN language_name varchar(255) AFTER language_code;

--;;

ALTER TABLE criteria_content ADD COLUMN language_name varchar(255) AFTER language_code;

--;;

ALTER TABLE issuer_content ADD COLUMN language_name varchar(255) AFTER language_code;

--;;

ALTER TABLE creator_content ADD COLUMN language_name varchar(255) AFTER language_code;
