
-- name: select-user-badges-all
-- get user's badges
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id

-- name: select-user-badges-valid
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, email, assertion_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id

-- name: select-user-badges-to-export
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, email, assertion_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id AND status != 'pending' AND assertion_url IS NOT NULL

-- name: select-user-badges-pending
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, email, assertion_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE user_id = :user_id AND status = 'pending'

-- name: select-public-badges
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, email, assertion_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       WHERE visibility = 'public' OR visibility = 'shared'
       ORDER BY ctime DESC

-- name: select-taglist
-- get tags by list of badge content ids
SELECT badge_id, tag FROM badge_tag WHERE badge_id IN (:badge_id)

-- name: select-user-owns-hosted-badge
-- check if user owns badge
SELECT COUNT(id) AS count FROM badge WHERE assertion_url = :assertion_url AND user_id = :user_id

-- name: select-user-owns-signed-badge
-- check if user owns badge
SELECT COUNT(id) AS count FROM badge WHERE assertion_json = :assertion_json AND user_id = :user_id

--name: replace-badge-content!
--save content of the badge
REPLACE INTO badge_content (id, name, description, image_file, criteria_html, criteria_markdown)
       VALUES (:id, :name, :description, :image_file, :criteria_html, :criteria_markdown)

--name: insert-badge<!
--save badge
INSERT INTO badge (user_id, email, assertion_url, assertion_jws, assertion_json, badge_url, issuer_url, criteria_url, badge_content_id, issuer_content_id, issued_on, expires_on, evidence_url, status, visibility, show_recipient_name, rating, ctime, mtime, deleted, revoked)
       VALUES (:user_id, :email, :assertion_url, :assertion_jws, :assertion_json, :badge_url, :issuer_url, :criteria_url, :badge_content_id, :issuer_content_id, :issued_on, :expires_on, :evidence_url, 'pending', 'private', 0, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), 0, 0)

--name: select-badge
--get badge by id
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, criteria_url, evidence_url, show_recipient_name, rating, ic.name AS issuer_name, ic.url AS issuer_url, ic.email AS issuer_contact, u.id, u.first_name, u.last_name FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       JOIN user AS u ON (u.id = badge.user_id)
       WHERE badge.id = :id

--name: replace-badge-tag!
REPLACE INTO badge_tag (badge_id, tag)
       VALUES (:badge_id, :tag)

--name: update-visibility!
--change badge visibility
UPDATE badge SET visibility = :visibility WHERE id = :id

--name: update-show-recipient-name!
--show/hide recipient name
UPDATE badge SET show_recipient_name = :show_recipient_name WHERE id = :id
