--name: select-users-from-connections-badge
SELECT user_id AS owner from social_connections_badge where badge_id = :badge_id

-- name: select-user-badges-all
-- get user's badges
SELECT ub.id, bc.name, bc.description, bc.image_file, ub.issued_on,
           ub.expires_on, ub.revoked, ub.visibility, ub.mtime, ub.status, ub.badge_id,
           b.issuer_verified, ic.name AS issuer_content_name, ic.url AS issuer_content_url
FROM user_badge ub
INNER JOIN badge b ON ub.badge_id = b.id
INNER JOIN badge_badge_content bb ON b.id = bb.badge_id
INNER JOIN badge_issuer_content bi ON b.id = bi.badge_id
INNER JOIN badge_content bc ON bb.badge_content_id = bc.id
INNER JOIN issuer_content ic ON bi.issuer_content_id = ic.id
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND ub.status != 'declined'
    AND bc.language_code = b.default_language_code
    AND ic.language_code = b.default_language_code

-- name: select-user-badges-to-export
SELECT ub.id,
       bc.name, bc.description, bc.image_file,
       ub.issued_on, ub.expires_on, ub.visibility, ub.mtime, ub.status, ub.badge_id,
       badge.issuer_verified, ub.email,
       ic.name AS issuer_content_name, ic.url AS issuer_content_url, ub.assertion_url
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_content AS bc ON (bc.id = ub.badge_id) AND bc.language_code = badge.default_language_code
JOIN issuer_content AS ic ON (ic.id = badge.id)
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND ub.status = 'accepted' AND ub.assertion_url IS NOT NULL AND ub.deleted = 0 AND ub.revoked = 0

-- name: select-user-badges-pending
SELECT ub.id, bc.name, bc.description, bc.image_file, ub.issued_on,
ub.expires_on, ub.visibility, ub.mtime, ub.badge_id, ub.assertion_url
FROM user_badge AS ub
INNER JOIN badge AS b ON (b.id = ub.badge_id)
INNER JOIN badge_badge_content bb ON (b.id = bb.badge_id)
INNER JOIN badge_content bc ON (bb.badge_content_id = bc.id)
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND ub.status = 'pending'
    AND bc.language_code = b.default_language_code

-- name: select-taglist
-- get tags by list of badge content ids
SELECT user_badge_id, tag FROM badge_tag WHERE user_badge_id IN (:user_badge_ids)

-- name: select-user-owns-hosted-badge
-- check if user owns badge
SELECT COUNT(id) AS count FROM user_badge WHERE assertion_url = :assertion_url AND user_id = :user_id AND status != 'declined' AND deleted = 0

-- name: select-user-owns-signed-badge
-- check if user owns badge
SELECT COUNT(id) AS count FROM user_badge WHERE assertion_json = :assertion_json AND user_id = :user_id AND status != 'declined' AND deleted = 0

-- name: select-user-owns-hosted-badge-id
-- check if user owns badge and returns id
SELECT id AS id FROM user_badge WHERE assertion_url = :assertion_url AND user_id = :user_id AND status != 'declined' AND deleted = 0

-- name: select-user-owns-signed-badge-id
-- check if user owns badge and returns id
SELECT id AS id FROM user_badge WHERE assertion_json = :assertion_json AND user_id = :user_id AND status != 'declined' AND deleted = 0

-- name: select-user-owns-badge-id
-- check if user owns badge and returns id
SELECT id FROM user_badge
WHERE user_id = :user_id AND status != 'declined' AND deleted = 0
AND (assertion_url = :assertion_url OR assertion_json = :assertion_json OR assertion_jws = :assertion_jws)


--name: replace-badge-content!
--save content of the badge
REPLACE INTO badge_content (id, name, description, image_file, language_code)
       VALUES (:id, :name, :description, :image_file :language_code)

--name: replace-criteria-content!
--save criteria content of the badge
REPLACE INTO criteria_content (id, html_content, markdown_content , language_code)
       VALUES (:id, :html_content, :markdown_content :language_code)

--name: old-insert-badge<!
--save badge
INSERT INTO badge (
       user_id,
       email,
       assertion_url,
       assertion_jws,
       assertion_json,
       badge_url,
       issuer_url,
       criteria_url,
       badge_content_id,
       issuer_content_id,
       issued_on,
       expires_on,
       evidence_url,
       status,
       visibility,
       show_recipient_name,
       rating,
       ctime,
       mtime,
       deleted,
       revoked,
       issuer_verified,
       criteria_content_id,
       creator_content_id)
       VALUES (
       :user_id,
       :email,
       :assertion_url,
       :assertion_jws,
       :assertion_json,
       :badge_url,
       :issuer_url,
       :criteria_url,
       :badge_content_id,
       :issuer_content_id,
       :issued_on,
       :expires_on,
       :evidence_url,
       :status,
       'private',
       0,
       NULL,
       UNIX_TIMESTAMP(),
       UNIX_TIMESTAMP(),
       0,
       0,
       :issuer_verified,
       :criteria_content_id, :creator_content_id)


--name: select-badge-visibility-recipients-count
select COUNT(visibility) AS visibility_count from user_badge where badge_id= :badge_id AND visibility <> "private";

--name: select-badge
--get badge by id
SELECT ub.id, ub.user_id,
ub.badge_id, ub.email,
ub.assertion_url, 
ub.assertion_json, ub.issued_on,
ub.expires_on, ub.status,
ub.visibility, ub.show_recipient_name,
ub.rating, ub.ctime,
ub.mtime, ub.deleted,
ub.revoked, ub.show_evidence,
bc.name, bc.description,
bc.image_file,
ic.name AS issuer_content_name,
ic.url AS issuer_content_url,
ic.description AS issuer_description,
ic.email AS issuer_contact,
ic.image_file AS issuer_image,
crc.name AS creator_name, crc.url AS creator_url,
crc.email AS creator_email,
crc.image_file AS creator_image,
crc.description AS creator_description,
u.id AS owner, u.first_name, u.last_name,
cc.markdown_text AS criteria_content,
cc.url AS criteria_url
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
LEFT JOIN badge_creator_content AS bcrc ON (bcrc.badge_id = ub.badge_id)
LEFT JOIN creator_content AS crc ON (crc.id = bcrc.creator_content_id)  AND crc.language_code = badge.default_language_code
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id) AND cc.language_code = badge.default_language_code
JOIN user AS u ON (u.id = ub.user_id)
WHERE ub.id = :id AND ub.deleted = 0
GROUP BY ub.id


--name: select-multi-language-user-badge
--get badge by id
SELECT ub.id, ub.user_id,
ub.badge_id, ub.email,
ub.assertion_url, 
ub.assertion_json, ub.issued_on,
ub.expires_on, ub.status,
ub.visibility, ub.show_recipient_name,
ub.rating, ub.ctime,
ub.mtime, ub.deleted,
ub.revoked, ub.show_evidence,
u.id AS owner, u.first_name, u.last_name
FROM user_badge AS ub
JOIN user AS u ON (u.id = ub.user_id)
WHERE ub.id = :id AND ub.deleted = 0
GROUP BY ub.id


--name: select-multi-language-badge-content
--get badge by id
SELECT
badge.id as badge_id, badge.default_language_code,
bc.language_code,
bc.name, bc.description,
bc.image_file,
ic.name AS issuer_content_name,
ic.url AS issuer_content_url,
ic.description AS issuer_description,
ic.email AS issuer_contact,
ic.image_file AS issuer_image,
crc.name AS creator_name, crc.url AS creator_url,
crc.email AS creator_email,
crc.image_file AS creator_image,
crc.description AS creator_description,
cc.markdown_text AS criteria_content,
cc.url AS criteria_url
FROM badge AS badge 
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id) 
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id)
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) 
LEFT JOIN badge_creator_content AS bcrc ON (bcrc.badge_id = badge.id)
LEFT JOIN creator_content AS crc ON (crc.id = bcrc.creator_content_id) 
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id) AND bc.language_code = cc.language_code AND ic.language_code = cc.language_code
WHERE badge.id = :id
GROUP BY bc.language_code, cc.language_code, ic.language_code

--name: replace-badge-tag!
REPLACE INTO badge_tag (user_badge_id, tag)
       VALUES (:user_badge_id, :tag)

--name: delete-badge-tags!
DELETE FROM badge_tag WHERE user_badge_id = :user_badge_id

--name: delete-badge-views!
DELETE FROM badge_view WHERE user_badge_id = :user_badge_id

--name: delete-badge-congratulations!
-- FIXME (rename badge_id -> user_badge_id)
DELETE FROM badge_congratulation WHERE user_badge_id = :user_badge_id

-- name: replace-issuer-content!
-- save issuer, replace if issuer exists already
REPLACE INTO issuer_content (id, name, url, description, image_file, email, revocation_list_url, language_code)
        VALUES (:id, :name, :url, :description, :image_file, :email, :revocation_list_url, :language_code);

-- name: replace-creator-content!
-- save badge original creator, replace if creator exists already
REPLACE INTO creator_content (id, url, name, description, image_file, email, json_url, language_code)
        VALUES (:id, :url, :name, :description, :image_file, :email, :json_url :language_code);

--name: update-visibility!
--change badge visibility
UPDATE user_badge SET visibility = :visibility WHERE id = :id

--name: update-revoked!
--change badge revoke status and last revoke check timestamp
UPDATE user_badge SET revoked = :revoked, last_checked = UNIX_TIMESTAMP() WHERE id = :id

--name: update-status!
--change badge status
UPDATE user_badge SET status = :status, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-show-recipient-name!
--show/hide recipient name
UPDATE user_badge SET show_recipient_name = :show_recipient_name WHERE id = :id

--name: update-show-evidence!
--show/hide evidence
UPDATE user_badge SET show_evidence = :show_evidence WHERE id = :id

--name: select-badge-settings
--get badge settings
SELECT ub.id, ub.issued_on,
ub.expires_on, ub.status,
ub.visibility, ub.show_recipient_name,
ub.rating,ub.revoked,
ub.show_evidence,
bc.name, bc.image_file,
ube.url,
cc.markdown_text AS criteria_content,
cc.url AS criteria_url,
ic.name AS issuer_content_name,
ic.url AS issuer_content_url,
ic.email AS issuer_contact,
ic.image_file AS issuer_image
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
LEFT JOIN user_badge_evidence AS ube ON (ube.user_badge_id = ub.id)
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id) AND cc.language_code = badge.default_language_code
WHERE ub.id = :id



--name: update-badge-settings!
--update badge settings
UPDATE user_badge SET visibility = :visibility, rating = :rating WHERE id = :id

--name: update-badge-published!
UPDATE badge SET published = :value where id = :badge_id

--name: update-badge-recipient-count!
UPDATE badge SET recipient_count = recipient_count + 1 where id = :badge_id

--name: update-badge-raiting!
--update badge raiting
UPDATE user_badge SET rating = :rating WHERE id = :id

--name: update-badge-set-deleted!
UPDATE user_badge SET deleted = 1, visibility = 'private' WHERE id = :id

--name: select-badges-images-names
SELECT ub.id,
bc.name, 
bc.image_file
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE ub.id IN (:ids)

--name: select-badges-by-tag-and-owner
SELECT ub.id, ub.issued_on,
ub.expires_on, ub.status,
ub.mtime, ub.badge_id,
bc.name, bc.description, bc.image_file,
bt.tag,
cc.markdown_text AS criteria_content
cc.url AS criteria_url,
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_tag AS bt ON bt.user_badge_id = ub.id
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id) AND cc.language_code = badge.default_language_code
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND bt.tag = :badge_tag

--name: select-badge-owner
--get badge owner's user_id
SELECT user_id FROM user_badge WHERE id = :id

--name: select-badge-owner-as-owner
--get badge owner's user_id
SELECT user_id AS owner FROM user_badge WHERE id = :id

--name: select-badge-congratulation
--get badge congratulation
SELECT user_badge_id, user_id, ctime FROM badge_congratulation WHERE user_badge_id = :user_badge_id AND user_id = :user_id

--name: insert-badge-congratulation<!
--add new badge congratulation
INSERT INTO badge_congratulation (user_badge_id, user_id, ctime) VALUES (:user_badge_id, :user_id, UNIX_TIMESTAMP())

--name: select-all-badge-congratulations
--get all users who congratulated another user from specific badge
SELECT u.id, first_name, last_name, profile_picture FROM user AS u
       JOIN badge_congratulation AS b ON u.id = b.user_id
       WHERE b.user_badge_id = :user_badge_id

--name: insert-badge-viewed!
--save badge view information
INSERT INTO badge_view (user_badge_id, user_id, ctime) VALUES (:user_badge_id, :user_id, UNIX_TIMESTAMP())

--name: select-badge-view-count
--get badge view count
-- FIXME (rename badge_id -> user_badge_id)
SELECT COUNT(id) AS count FROM badge_view WHERE user_badge_id = :user_badge_id

--name: select-badge-recipient-count
--get badge badge recipient count
SELECT recipient_count FROM badge WHERE id = :badge_id

--name: select-user-badge-count
--get user's badge count
SELECT COUNT(id) as count FROM user_badge WHERE user_id = :user_id AND deleted = 0 AND status = 'accepted'

--name: select-user-expired-badge-count
--get user's expired badge count
SELECT COUNT(id) as count FROM user_badge WHERE user_id = :user_id AND deleted = 0 AND status = 'accepted' AND expires_on <= UNIX_TIMESTAMP()

--name: select-badge-views-stats
--get user's badge view stats
SELECT ub.id,
bc.name, bc.image_file,
SUM(bv.id IS NOT NULL AND bv.user_id IS NOT NULL) AS reg_count, SUM(bv.id IS NOT NULL AND bv.user_id IS NULL) AS anon_count, MAX(bv.ctime) AS latest_view
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_view AS bv ON ub.id = bv.user_badge_id
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND ub.status = 'accepted'
GROUP BY ub.id, bc.name, bc.image_file
ORDER BY latest_view DESC



--name: select-badge-congratulations-stats
--get user's badge congratulations stats
SELECT ub.id,
bc.name, bc.image_file,
COUNT(bco.user_id) AS congratulation_count,
MAX(bco.ctime) AS latest_congratulation 
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_congratulation AS bco ON ub.id = bco.user_badge_id
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND ub.status = 'accepted'
GROUP BY ub.id, bc.name, bc.image_file
ORDER BY latest_congratulation DESC

--name: select-badge-issuer-stats
--get user's badge issuer stats
SELECT ub.id,
bc.name, bc.image_file,
ic.name AS issuer_content_name,
ic.url AS issuer_content_url
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN issuer_content AS ic ON (ic.id = ub.badge_id) AND ic.language_code = badge.default_language_code
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND ub.status = 'accepted'
ORDER BY ub.id, bc.name, bc.image_file

--name: update-badge-set-verified!
--update verification status of the issuer of the badge
UPDATE badge SET issuer_verified = :issuer_verified, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: select-badge-assertion-url
SELECT assertion_url FROM user_badge WHERE id = :id AND user_id = :user_id

--name: select-badge-id-by-old-id-user-id
SELECT id FROM user_badge WHERE user_id = :user_id AND old_id = :old_id

--name: select-badge-content-id-by-old-id
SELECT badge_id FROM user_badge WHERE old_id = :old_id

--name: select-badge-id-by-user-badge-id
SELECT badge_id FROM user_badge WHERE id = :user_badge_id


-- name: insert-badge-content!
INSERT IGNORE INTO badge_content (id, name, description, image_file, language_code)
       VALUES (:id, :name, :description, :image_file , :language_code)

-- name: insert-badge-content-tag!
INSERT IGNORE INTO badge_content_tag (badge_content_id, tag)
       VALUES (:badge_content_id, :tag)


--name: insert-badge-content-alignment!
INSERT IGNORE INTO badge_content_alignment (badge_content_id, name, url, description)
       VALUES (:badge_content_id, :name, :url, :description)

-- name: insert-criteria-content!
INSERT IGNORE INTO criteria_content (id, language_code, url, markdown_text)
       VALUES (:id, :language_code, :url, :markdown_text)

-- name: insert-issuer-content!
INSERT IGNORE INTO issuer_content (id, name, url, description, image_file, email, revocation_list_url, language_code)
        VALUES (:id, :name, :url, :description, :image_file, :email, :revocation_list_url, :language_code);

-- name: insert-creator-content!
INSERT IGNORE INTO creator_content (id, name, url, description, image_file, email, json_url, language_code)
        VALUES (:id, :name, :url, :description, :image_file, :email, :json_url, :language_code);




--name: select-user-events
-- EVENTS
SELECT se.subject, se.verb, se.object, se.ctime, se.type, seo.event_id, seo.last_checked, bc.name, bc.image_file, seo.hidden FROM social_event_owners AS seo
     JOIN social_event AS se ON seo.event_id = se.id
     JOIN badge AS badge ON (badge.id = se.object)
     JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
     JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
     JOIN social_connections_badge AS scb ON :user_id = scb.user_id
     WHERE owner = :user_id AND se.type = 'badge' AND se.object = scb.badge_id
     ORDER BY se.ctime DESC
     LIMIT 1000

--name: select-messages-with-badge-id
-- EVENTS
SELECT bmv.badge_id, bm.user_id, bm.message, bm.ctime, u.first_name, u.last_name, u.profile_picture, bmv.mtime AS last_viewed from badge_message as bm
JOIN user AS u ON (u.id = bm.user_id)
JOIN badge_message_view AS bmv ON bm.badge_id = bmv.badge_id AND :user_id =  bmv.user_id
WHERE bm.badge_id IN (:badge_ids) AND bm.deleted = 0
ORDER BY bm.ctime DESC
LIMIT 100

--name: insert-badge!
--save badge content
INSERT IGNORE INTO badge (
    id, remote_url, remote_id, remote_issuer_id, issuer_verified,
    default_language_code,
    published, last_received, recipient_count
) VALUES (
    :id, :remote_url, :remote_id, :remote_issuer_id, :issuer_verified,
    :default_language_code,
    0, UNIX_TIMESTAMP(), 0
);

--name: insert-user-badge<!
--save user badge
INSERT INTO user_badge (
    badge_id, user_id, email,
    assertion_url, assertion_jws, assertion_json,
    issued_on, expires_on, status, visibility,
    show_recipient_name, rating, ctime, mtime, deleted, revoked
) VALUES (
    :badge_id, :user_id, :email,
    :assertion_url, :assertion_jws, :assertion_json,
    :issued_on, :expires_on, :status, 'private',
    0, NULL, UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), 0, 0
);
