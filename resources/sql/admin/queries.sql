--name: select-user-language
SELECT language FROM user WHERE id = :id

--name: total-user-count
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND deleted = 0;

--name: total-user-count-fix
-- Count all users both activated and not
SELECT COUNT(DISTINCT id) AS count FROM user WHERE deleted = 0;

--name: activated-user-count
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND deleted = 0;

--name: not-activated-user-count
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 0 AND deleted = 0;

--name: public-user-count
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND profile_visibility = 'public';

--name: internal-user-count
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND profile_visibility = 'internal';

--name: count-logged-users-after-date
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND last_login > :time AND deleted = 0;

--name: count-logged-users-after-date-range-REMOVE
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND last_login > :from AND last_login < :to AND deleted = 0;

--name: count-registered-users-after-date
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND ctime > :time;

--name: count-registered-users-after-date-fix
SELECT COUNT(DISTINCT id) AS count FROM user WHERE ctime > :time;

--name: count-all-badges
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND revoked = 0 AND status != 'declined';

--name: count-all-badges-fix
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0  AND revoked = 0;

--name: count-pending-badges
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND status = 'pending' AND revoked = 0;

--name: count-declined-badges
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND status = 'declined' AND revoked = 0;

--name: count-accepted-badges
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND status = 'accepted' AND revoked = 0;

--name: count-all-badges-after-date
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE ctime > :time AND deleted = 0 AND status != 'declined';

--name: count-private-badges
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND visibility = 'private' AND revoked = 0;

--name: count-internal-badges
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND visibility = 'internal' AND revoked = 0;

--name: count-public-badges
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND visibility = 'public' AND revoked = 0;

--name: count-private-pages
SELECT COUNT(DISTINCT id) AS count FROM page WHERE deleted = 0 AND visibility = 'private';

--name: count-internal-pages
SELECT COUNT(DISTINCT id) AS count FROM page WHERE deleted = 0 AND visibility = 'internal';

--name: count-public-pages
SELECT COUNT(DISTINCT id) AS count FROM page WHERE deleted = 0 AND visibility = 'public';

--name: count-all-badges-after-date-fix
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE ctime > :time AND deleted = 0 AND revoked = 0;

--name: count-all-pages
SELECT COUNT(DISTINCT id) AS count FROM page WHERE  deleted = 0;

--name: count-all-pages-after-date
SELECT COUNT(DISTINCT id) AS count FROM page WHERE ctime > :time AND deleted = 0;

--name: select-user-admin
SELECT role FROM user WHERE id= :id;

--name: update-page-visibility!
UPDATE page SET visibility = 'private', mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-badge-visibility!
UPDATE user_badge SET visibility = 'private', mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-user-visibility!
UPDATE user SET profile_visibility  = 'internal', mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-user-badge-visibility-by-badge-id!
UPDATE user_badge SET visibility = 'private', mtime = UNIX_TIMESTAMP()  WHERE badge_id= :badge_id

--name: update-badge-visibility-by-badge-id!
UPDATE badge SET published = 0  WHERE id= :badge_id

--name: insert-report-ticket<!
--add new report ticket
INSERT INTO report_ticket (description, report_type, item_id, item_url, item_name, item_type, reporter_id, item_content_id, ctime, mtime) VALUES (:description, :report_type, :item_id, :item_url, :item_name, :item_type, :reporter_id, :item_content_id, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: select-tickets
--get tickets with open status
SELECT t.id, t.description, t.report_type, t.item_id, t.item_url, t.item_name, t.item_type, t.reporter_id, u.first_name, u.last_name, t.item_content_id, t.ctime, t.status FROM report_ticket t
       JOIN user AS u ON (u.id = t.reporter_id)
       WHERE status = 'open'
       ORDER BY t.ctime DESC

--name: select-closed-tickets
--get tickets with closed status
SELECT t.id, t.description, t.report_type, t.item_id, t.item_url, t.item_name, t.item_type, t.reporter_id, u.first_name, u.last_name, t.item_content_id, t.ctime, t.status, t.mtime FROM report_ticket t
       JOIN user AS u ON (u.id = t.reporter_id)
       WHERE status = 'closed'
       ORDER BY t.mtime DESC

--name: update-ticket-status!
UPDATE report_ticket SET status  = :status, mtime = UNIX_TIMESTAMP() WHERE id = :id


--name: update-badge-deleted!-REMOVE
UPDATE badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-badge-deleted!
UPDATE user_badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-page-deleted!
UPDATE page SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-user-deleted!
UPDATE user SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-user-undeleted!
UPDATE user SET deleted = 0, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: select-user-and-email
SELECT u.first_name, u.last_name, ue.email, u.language, u.activated, ue.verification_key  FROM user AS u
JOIN user_email AS ue ON u.id = ue.user_id
WHERE u.id= :id AND ue.primary_address = 1

--name: select-users-email
SELECT email FROM user_email
       WHERE user_id IN (:user_id) AND primary_address = 1

--name: select-user-id-by-badge-id-REMOVE
SELECT user_id FROM badge WHERE id=:id

--name: select-user-id-by-badge-id
SELECT user_id FROM user_badge WHERE id = :id

--name: select-users-id-by-badge-id
select user_id from user_badge WHERE badge_id = :badge_id AND deleted = 0



--name: update-user-pages-set-private!
UPDATE page SET visibility = 'private' WHERE user_id = :user_id

--name: update-user-badges-set-private!
UPDATE user_badge SET visibility = 'private' WHERE user_id = :user_id

--name: delete-user-badge-views!
DELETE FROM badge_view WHERE user_id = :user_id

--name: delete-user-badge-congratulations!
DELETE FROM badge_congratulation WHERE user_id = :user_id

--name: update-badge-deleted-by-badge-id!
UPDATE user_badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE badge_id = :badge_id

--name: update-badge-deleted-by-badge-content-id!
UPDATE user_badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE badge_id = :badge_id

--name: select-users-name-and-email
SELECT first_name, last_name, ue.email, deleted FROM user AS u
JOIN user_email AS ue ON u.id = ue.user_id

--name: select-email-by-user-ids
SELECT user_id, GROUP_CONCAT(email, primary_address) AS email from user_email where user_id IN (:ids)
GROUP BY user_id


--name: delete-email-addresses!
DELETE FROM user_email WHERE user_id = :user_id

--name: delete-no-activated-user!
DELETE FROM user WHERE id = :id and activated = 0

-- name: delete-email-no-verified-address!
DELETE FROM user_email WHERE email = :email AND user_id = :user_id AND primary_address = 0 AND verified = 0


--name: insert-config<!
--Add or update config
REPLACE INTO config (name, value)
                   VALUES (:name, :value)


--name: select-name-value-config
SELECT name, value FROM config WHERE name = :name


--name: select-admin-users-id
SELECT id AS owner FROM user WHERE role='admin'


--name: select-admin-events
SELECT  se.subject, se.verb, se.object, se.ctime, seo.event_id, seo.last_checked, seo.hidden, re.item_name, re.report_type FROM social_event_owners AS seo
     JOIN social_event AS se ON seo.event_id = se.id
     LEFT JOIN report_ticket AS re  ON se.object = re.id
     WHERE seo.owner = :user_id AND se.verb = 'ticket' AND se.type = 'admin' AND re.status = 'open'
     ORDER BY se.ctime DESC
     LIMIT 1000;

--name: select-user
-- get user by id
SELECT id, first_name, last_name, country, language, profile_visibility, profile_picture, role, about, email_notifications, activated FROM user WHERE id = :id AND deleted = 0

--name: update-admin-to-user!
UPDATE user SET role = 'user' WHERE id = :id

--name: update-user-to-admin!
UPDATE user SET role = 'admin' WHERE id = :id

--name: select-admin-count
SELECT COUNT(*) AS count FROM user WHERE role = 'admin' AND activated = 1 AND deleted = 0;

--name: count-badges-issued-from-url
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND revoked = 0 AND assertion_url LIKE :url;

--name: count-badge-issuers
SELECT COUNT(DISTINCT name) AS count FROM issuer_content;

--name: count-badge-issuers-after-date
SELECT COUNT(*) AS count FROM new_issuer_history WHERE ctime > :time;

--name:select-user-ids-and-badge-count
SELECT u.id, COUNT(ub.id) AS badge_count FROM user u
LEFT JOIN user_badge ub ON u.id=ub.user_id WHERE u.activated = 1 AND ub.status != 'declined' AND ub.revoked = 0 AND ub.deleted = 0
GROUP BY u.id;
