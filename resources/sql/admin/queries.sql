--name: select-user-language
SELECT language FROM user WHERE id = :id

--name: total-user-count
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1;

--name: count-logged-users-after-date
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND last_login > :time;

--name: count-registered-users-after-date
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND ctime > :time;

--name: count-all-badges
SELECT COUNT(DISTINCT id) AS count FROM badge;

--name: count-all-badges-after-date
SELECT COUNT(DISTINCT id) AS count FROM badge WHERE ctime > :time;

--name: count-all-pages
SELECT COUNT(DISTINCT id) AS count FROM page;


--name: select-user-admin
SELECT role FROM user WHERE id= :id;

--name: update-page-visibility!
UPDATE page SET visibility = 'private', mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-badge-visibility!
UPDATE badge SET visibility = 'private', mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-user-visibility!
UPDATE user SET profile_visibility  = 'internal', mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-badges-visibility!
UPDATE badge SET visibility = 'private', mtime = UNIX_TIMESTAMP()  WHERE badge_content_id= :badge_content_id


--name: get-badges-id-by-badges-content-id
SELECT id FROM badge WHERE badge_content_id= :badge_content_id AND visibility != 'private';
