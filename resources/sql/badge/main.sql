
-- name: select-user-badges-all
-- get user's badges
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, status, badge_content_id FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id AND deleted = 0

-- name: select-user-badges-to-export
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, status, badge_content_id, email, assertion_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id AND status = 'accepted' AND assertion_url IS NOT NULL AND deleted = 0

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
INSERT INTO badge (user_id, email, assertion_url, assertion_jws, assertion_json, badge_url, issuer_url, criteria_url, badge_content_id, issuer_content_id, issued_on, expires_on, evidence_url, status, visibility, show_recipient_name, rating, ctime, mtime, deleted, revoked)
       VALUES (:user_id, :email, :assertion_url, :assertion_jws, :assertion_json, :badge_url, :issuer_url, :criteria_url, :badge_content_id, :issuer_content_id, :issued_on, :expires_on, :evidence_url, :status, 'private', 0, NULL, UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), 0, 0)

--name: select-badge
--get badge by id
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, criteria_url, evidence_url, show_recipient_name, rating, status, ic.name AS issuer_name, ic.url AS issuer_url, ic.email AS issuer_contact, u.id AS owner, u.first_name, u.last_name FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       JOIN user AS u ON (u.id = badge.user_id)
       WHERE badge.id = :id

--name: replace-badge-tag!
REPLACE INTO badge_tag (badge_id, tag)
       VALUES (:badge_id, :tag)

--name: delete-badge-tags!
DELETE FROM badge_tag WHERE badge_id = :badge_id

-- name: replace-issuer-content!
-- save issuer, replace if issuer exists already
REPLACE INTO issuer_content (id,name,url,description,image_file,email,revocation_list_url)
        VALUES (:id, :name, :url, :description, :image_file, :email, :revocation_list_url);

--name: update-visibility!
--change badge visibility
UPDATE badge SET visibility = :visibility WHERE id = :id

--name: update-status!
--change badge status
UPDATE badge SET status = :status WHERE id = :id

--name: update-show-recipient-name!
--show/hide recipient name
UPDATE badge SET show_recipient_name = :show_recipient_name WHERE id = :id

--name: select-badge-settings
--get badge settings
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, criteria_url, cc.html_content AS criteria_html, evidence_url, rating, ic.name AS issuer_name, ic.url AS issuer_url, ic.email AS issuer_contact FROM badge
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
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, status, badge_content_id, bt.tag FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN badge_tag AS bt ON bt.badge_id = badge.id
       WHERE user_id = :user_id AND deleted = 0 AND bt.tag = :badge_tag

--name: select-badge-owner
--get badge owner's user_id
SELECT user_id FROM badge WHERE id = :id

