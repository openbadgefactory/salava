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

--name: insert-report-ticket<!
--add new report ticket
INSERT INTO report_ticket (description, report_type, item_id, item_url, item_name, item_type, reporter_id, item_content_id, ctime, mtime) VALUES (:description, :report_type, :item_id, :item_url, :item_name, :item_type, :reporter_id, :item_content_id, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: select-tickets
--get tickets with open status
SELECT t.id, t.description, t.report_type, t.item_id, t.item_url, t.item_name, t.item_type, t.reporter_id, u.first_name, u.last_name, t.item_content_id, t.ctime FROM report_ticket t
       JOIN user AS u ON (u.id = t.reporter_id)
       WHERE status = 'open'
       ORDER BY t.ctime DESC

--name: update-ticket-status!
UPDATE report_ticket SET status  = 'closed', mtime = UNIX_TIMESTAMP() WHERE id = :id


--name: update-badge-deleted!
UPDATE badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: select-user-and-email
SELECT first_name, last_name, ue.email FROM user AS u
JOIN user_email AS ue ON u.id = ue.user_id
WHERE id=:id AND ue.primary_address=1

--name: select-user-id-by-badge-id
SELECT user_id FROM badge WHERE id=:id
