
-- name: select-user-badges-all
-- get user's badges
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, revoked, visibility, mtime, status, badge_content_id, badge_url, issuer_url, issuer_verified, ic.name AS issuer_content_name, ic.url AS issuer_content_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       WHERE user_id = :user_id AND deleted = 0 AND status != "declined"

-- name: select-user-badges-to-export
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, status, badge_content_id, email, assertion_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id AND status = 'accepted' AND assertion_url IS NOT NULL AND deleted = 0 AND revoked = 0

-- name: select-user-badges-pending
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, assertion_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id AND status = 'pending' AND deleted = 0

-- name: select-taglist
-- get tags by list of badge content ids
SELECT badge_id, tag FROM badge_tag WHERE badge_id IN (:badge_ids)

-- name: select-user-owns-hosted-badge
-- check if user owns badge
SELECT COUNT(id) AS count FROM badge WHERE assertion_url = :assertion_url AND user_id = :user_id AND status != 'declined' AND deleted = 0

-- name: select-user-owns-signed-badge
-- check if user owns badge
SELECT COUNT(id) AS count FROM badge WHERE assertion_json = :assertion_json AND user_id = :user_id AND status != 'declined' AND deleted = 0

--name: replace-badge-content!
--save content of the badge
REPLACE INTO badge_content (id, name, description, image_file)
       VALUES (:id, :name, :description, :image_file)

--name: replace-criteria-content!
--save criteria content of the badge
REPLACE INTO criteria_content (id, html_content, markdown_content)
       VALUES (:id, :html_content, :markdown_content)

--name: insert-badge<!
--save badge
INSERT INTO badge (user_id, email, assertion_url, assertion_jws, assertion_json, badge_url, issuer_url, criteria_url, badge_content_id, issuer_content_id, issued_on, expires_on, evidence_url, status, visibility, show_recipient_name, rating, ctime, mtime, deleted, revoked, issuer_verified, criteria_content_id)
       VALUES (:user_id, :email, :assertion_url, :assertion_jws, :assertion_json, :badge_url, :issuer_url, :criteria_url, :badge_content_id, :issuer_content_id, :issued_on, :expires_on, :evidence_url, :status, 'private', 0, NULL, UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), 0, 0, :issuer_verified, :criteria_content_id)

--name: select-badge
--get badge by id
SELECT badge.id, badge_content_id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, criteria_url, evidence_url, show_recipient_name, rating, status, assertion_url, assertion_json, revoked, last_checked, badge_url, issuer_verified, badge.ctime, badge.mtime, ic.name AS issuer_content_name, ic.url AS issuer_content_url, ic.email AS issuer_contact, ic.image_file AS issuer_image, u.id AS owner, u.first_name, u.last_name, cc.html_content FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       LEFT JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       LEFT JOIN criteria_content AS cc ON (cc.id = badge.criteria_content_id)
       JOIN user AS u ON (u.id = badge.user_id)
       WHERE badge.id = :id

--name: replace-badge-tag!
REPLACE INTO badge_tag (badge_id, tag)
       VALUES (:badge_id, :tag)

--name: delete-badge-tags!
DELETE FROM badge_tag WHERE badge_id = :badge_id

--name: delete-badge-views!
DELETE FROM badge_view WHERE badge_id = :badge_id

--name: delete-badge-congratulations!
DELETE FROM badge_congratulation WHERE badge_id = :badge_id

-- name: replace-issuer-content!
-- save issuer, replace if issuer exists already
REPLACE INTO issuer_content (id,name,url,description,image_file,email,revocation_list_url)
        VALUES (:id, :name, :url, :description, :image_file, :email, :revocation_list_url);

--name: update-visibility!
--change badge visibility
UPDATE badge SET visibility = :visibility WHERE id = :id

--name: update-revoked!
--change badge revoke status and last revoke check timestamp
UPDATE badge SET revoked = :revoked, last_checked = UNIX_TIMESTAMP() WHERE id = :id

--name: update-status!
--change badge status
UPDATE badge SET status = :status WHERE id = :id

--name: update-show-recipient-name!
--show/hide recipient name
UPDATE badge SET show_recipient_name = :show_recipient_name WHERE id = :id

--name: update-show-evidence!
--show/hide evidence
UPDATE badge SET show_evidence = :show_evidence WHERE id = :id

--name: select-badge-settings
--get badge settings
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, criteria_url, cc.html_content AS criteria_html, evidence_url, rating, revoked, ic.name AS issuer_content_name, ic.url AS issuer_content_url, ic.email AS issuer_contact, ic.image_file AS issuer_image FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       LEFT JOIN criteria_content AS cc ON (cc.id = badge.criteria_content_id)
       WHERE badge.id = :id

--name: update-badge-settings!
--update badge settings
UPDATE badge SET visibility = :visibility, rating = :rating, evidence_url = :evidence_url WHERE id = :id

--name: update-badge-set-deleted!
UPDATE badge SET deleted = 1, visibility = 'private' WHERE id = :id

--name: select-badges-images-names
SELECT b.id, bc.name, bc.image_file FROM badge AS b JOIN badge_content AS bc ON b.badge_content_id = bc.id WHERE b.id IN (:ids)

--name: select-badges-by-tag-and-owner
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, status, criteria_url, badge_content_id, bt.tag, cc.html_content FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN criteria_content AS cc ON (cc.id = badge.criteria_content_id)
       JOIN badge_tag AS bt ON bt.badge_id = badge.id
       WHERE user_id = :user_id AND deleted = 0 AND bt.tag = :badge_tag

--name: select-badge-owner
--get badge owner's user_id
SELECT user_id FROM badge WHERE id = :id

--name: select-badge-congratulation
--get badge congratulation
SELECT badge_id, user_id, ctime FROM badge_congratulation WHERE badge_id = :badge_id AND user_id = :user_id

--name: insert-badge-congratulation!
--add new badge congratulation
INSERT INTO badge_congratulation (badge_id, user_id, ctime) VALUES (:badge_id, :user_id, UNIX_TIMESTAMP())

--name: select-all-badge-congratulations
--get all users who congratulated another user from specific badge
SELECT u.id, first_name, last_name, profile_picture FROM user AS u
       JOIN badge_congratulation AS b ON u.id = b.user_id
       WHERE b.badge_id = :badge_id

--name: insert-badge-viewed!
--save badge view information
INSERT INTO badge_view (badge_id, user_id, ctime) VALUES (:badge_id, :user_id, UNIX_TIMESTAMP())

--name: select-badge-view-count
--get badge view count
SELECT COUNT(id) AS count FROM badge_view WHERE badge_id = :badge_id

--name: select-badge-recipient-count
--get badge badge recipient count
SELECT COUNT(DISTINCT user_id) AS recipient_count FROM badge WHERE badge_content_id = :badge_content_id AND (visibility = 'public' OR visibility = :visibility) AND status='accepted' and deleted = 0

--name: select-user-badge-count
--get user's badge count
SELECT COUNT(id) as count FROM badge WHERE user_id = :user_id AND deleted = 0 AND status = 'accepted'

--name: select-user-expired-badge-count
--get user's expired badge count
SELECT COUNT(id) as count FROM badge WHERE user_id = :user_id AND deleted = 0 AND status = 'accepted' AND expires_on <= UNIX_TIMESTAMP()

--name: select-badge-views-stats
--get user's badge view stats
SELECT b.id, bc.name, bc.image_file, SUM(bv.id IS NOT NULL AND bv.user_id IS NOT NULL) AS reg_count, SUM(bv.id IS NOT NULL AND bv.user_id IS NULL) AS anon_count, MAX(bv.ctime) AS latest_view FROM badge AS b
       JOIN badge_view AS bv ON b.id = bv.badge_id
       JOIN badge_content AS bc ON b.badge_content_id = bc.id
       WHERE b.user_id = :user_id AND b.deleted = 0 AND b.status = 'accepted'
       GROUP BY b.id
       ORDER BY latest_view DESC

--name: select-badge-congratulations-stats
--get user's badge congratulations stats
SELECT b.id, bc.name, bc.image_file, COUNT(bco.user_id) AS congratulation_count, MAX(bco.ctime) AS latest_congratulation FROM badge AS b
       JOIN badge_congratulation AS bco ON b.id = bco.badge_id
       JOIN badge_content AS bc ON b.badge_content_id = bc.id
       WHERE b.user_id = :user_id AND b.deleted = 0 AND b.status = 'accepted'
       GROUP BY b.id
       ORDER BY latest_congratulation DESC

--name: select-badge-issuer-stats
--get user's badge issuer stats
SELECT b.id, bc.name, bc.image_file, b.issuer_content_id, ic.name AS issuer_content_name, ic.url AS issuer_content_url FROM badge AS b
       JOIN badge_content AS bc ON b.badge_content_id = bc.id
       JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
       WHERE b.user_id = :user_id AND b.deleted = 0 AND b.status = 'accepted'
       ORDER BY ic.name

--name: update-badge-set-verified!
--update verification status of the issuer of the badge
UPDATE badge SET issuer_verified = :issuer_verified, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: select-badge-assertion-url
SELECT assertion_url FROM badge WHERE id = :id AND user_id = :user_id