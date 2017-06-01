ALTER TABLE badge_content DROP COLUMN language_code, DROP COLUMN language_name;

--;;

ALTER TABLE issuer_content DROP COLUMN language_code, DROP COLUMN language_name;

--;;

ALTER TABLE creator_content DROP COLUMN language_code, DROP COLUMN language_name;

--;;

DROP TABLE badge;

--;;

DROP TABLE user_badge;

--;;

DROP TABLE user_badge_evidence;

--;;

DROP TABLE criteria_content;

--;;

DROP TABLE badge_badge_content;

--;;

DROP TABLE badge_criteria_content;

--;;

DROP TABLE badge_issuer_content;

--;;

DROP TABLE badge_creator_content;

--;;

CREATE TABLE badge (id bigint UNSIGNED PRIMARY KEY NOT NULL AUTO_INCREMENT) AS SELECT * FROM badge_old;

--;;

CREATE TABLE criteria_content (id varchar(255) PRIMARY KEY NOT NULL) AS SELECT * FROM criteria_content_old;

--;;

DROP TABLE badge_old;

--;;

DROP TABLE criteria_content_old;
