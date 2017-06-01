RENAME TABLE badge TO badge_old;

--;;

CREATE TABLE badge (
    id varchar(255) PRIMARY KEY NOT NULL,
    remote_url varchar(500) DEFAULT NULL,
    remote_id varchar(255) DEFAULT NULL,
    remote_issuer_id varchar(255) DEFAULT NULL,
    issuer_verified tinyint DEFAULT 0,
    default_language_code varchar(255) DEFAULT '',
    default_language_name varchar(255) DEFAULT '',
    published tinyint UNSIGNED DEFAULT 0,
    last_received bigint UNSIGNED DEFAULT NULL,
    recipient_count bigint UNSIGNED DEFAULT NULL
);

--;;

CREATE TABLE badge_badge_content (
    badge_id varchar(255) NOT NULL,
    badge_content_id varchar(255) NOT NULL,
    PRIMARY KEY (badge_id, badge_content_id)
);

--;;

CREATE TABLE badge_criteria_content (
    badge_id varchar(255) NOT NULL,
    criteria_content_id varchar(255) NOT NULL,
    PRIMARY KEY (badge_id, criteria_content_id)
);

--;;

CREATE TABLE badge_issuer_content (
    badge_id varchar(255) NOT NULL,
    issuer_content_id varchar(255) NOT NULL,
    PRIMARY KEY (badge_id, issuer_content_id)
);

--;;

CREATE TABLE badge_creator_content (
    badge_id varchar(255) NOT NULL,
    creator_content_id varchar(255) NOT NULL,
    PRIMARY KEY (badge_id, creator_content_id)
);

--;;

RENAME TABLE criteria_content TO criteria_content_old;

--;;

CREATE TABLE criteria_content (
    id varchar(255) NOT NULL PRIMARY KEY,
    language_code varchar(255) DEFAULT '',
    language varchar(255) DEFAULT '',
    url varchar(255) NOT NULL,
    markdown_text mediumtext
);

--;;

CREATE TABLE user_badge (
    id bigint UNSIGNED PRIMARY KEY NOT NULL AUTO_INCREMENT,
    user_id bigint UNSIGNED DEFAULT NULL,
    badge_id varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    assertion_url varchar(500) COLLATE utf8_unicode_ci DEFAULT NULL,
    assertion_jws text,
    assertion_json mediumtext,
    issued_on bigint UNSIGNED DEFAULT NULL,
    expires_on bigint UNSIGNED DEFAULT NULL,
    status enum('pending','accepted','declined') DEFAULT 'pending',
    visibility enum('private','internal','public') DEFAULT 'private',
    show_recipient_name tinyint DEFAULT 0,
    rating smallint DEFAULT NULL,
    ctime bigint UNSIGNED NOT NULL,
    mtime bigint UNSIGNED NOT NULL,
    deleted tinyint DEFAULT 0,
    revoked tinyint DEFAULT 0,
    show_evidence tinyint DEFAULT 0,
    last_checked bigint UNSIGNED DEFAULT NULL,
    old_id bigint UNSIGNED DEFAULT NULL
);

--;;

CREATE TABLE user_badge_evidence (
    id bigint unsigned PRIMARY KEY NOT NULL AUTO_INCREMENT,
    user_badge_id bigint unsigned NOT NULL,
    url varchar(500) DEFAULT NULL,
    narrative mediumtext,
    name text,
    description text,
    genre text,
    audience text,
    ctime bigint unsigned NOT NULL,
    mtime bigint unsigned NOT NULL
);

--;;

ALTER TABLE badge_content
    ADD COLUMN language_name varchar(255) DEFAULT '' AFTER id,
    ADD COLUMN language_code varchar(255) DEFAULT '' AFTER id;

--;;

ALTER TABLE issuer_content
    ADD COLUMN language_name varchar(255) DEFAULT '' AFTER id,
    ADD COLUMN language_code varchar(255) DEFAULT '' AFTER id;

--;;

ALTER TABLE creator_content
    ADD COLUMN language_name varchar(255) DEFAULT '' AFTER id,
    ADD COLUMN language_code varchar(255) DEFAULT '' AFTER id;

--;;

INSERT IGNORE INTO badge (id, published, last_received)
SELECT badge_content_id, 1, MAX(ctime) FROM badge_old
WHERE status = 'accepted' AND visibility != 'private'
AND deleted = 0 AND revoked = 0
GROUP BY badge_content_id;

--;;

INSERT INTO badge (id, recipient_count)
SELECT badge_content_id, COUNT(*) FROM badge_old GROUP BY badge_content_id
ON DUPLICATE KEY UPDATE recipient_count = VALUES(recipient_count);

--;;

INSERT IGNORE INTO badge_badge_content (badge_id, badge_content_id)
SELECT badge_content_id, badge_content_id FROM badge_old;

--;;

INSERT IGNORE INTO badge_criteria_content (badge_id, criteria_content_id)
SELECT badge_content_id, criteria_content_id FROM badge_old ORDER BY ctime DESC;

--;;

INSERT IGNORE INTO badge_issuer_content (badge_id, issuer_content_id)
SELECT badge_content_id, issuer_content_id FROM badge_old ORDER BY ctime DESC;

--;;

INSERT IGNORE INTO badge_creator_content (badge_id, creator_content_id)
SELECT badge_content_id, creator_content_id FROM badge_old
WHERE creator_content_id IS NOT NULL ORDER BY ctime DESC;

--;;

INSERT INTO user_badge (
    id,
    user_id,
    badge_id,
    email,
    assertion_url,
    assertion_jws,
    assertion_json,
    issued_on,
    expires_on,
    status,
    visibility,
    show_recipient_name,
    rating,
    ctime,
    mtime,
    deleted,
    revoked,
    show_evidence,
    last_checked,
    old_id
)
SELECT
    id,
    user_id,
    badge_content_id,
    email,
    assertion_url,
    assertion_jws,
    assertion_json,
    issued_on,
    expires_on,
    status,
    visibility,
    show_recipient_name,
    rating,
    ctime,
    mtime,
    deleted,
    revoked,
    show_evidence,
    last_checked,
    old_id
FROM badge_old;

--;;

INSERT INTO user_badge_evidence (user_badge_id, url, ctime, mtime)
SELECT id, evidence_url, ctime, mtime FROM badge_old;

--;;

INSERT IGNORE INTO criteria_content (id, url, markdown_text)
SELECT c.id, b.criteria_url, c.markdown_content FROM badge_old b
INNER JOIN criteria_content_old c ON b.criteria_content_id = c.id;
