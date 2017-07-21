ALTER TABLE badge DROP COLUMN default_language_name;

--;;

ALTER TABLE badge_content DROP COLUMN language_name;

--;;

ALTER TABLE criteria_content DROP COLUMN language_name;

--;;

ALTER TABLE issuer_content DROP COLUMN language_name;

--;;

ALTER TABLE creator_content DROP COLUMN language_name;
