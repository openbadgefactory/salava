-- name: select-verified-email-address
-- Get user id by email address
SELECT user_id FROM user_email WHERE email = :email AND verified = 1

-- name: select-public-badge-count
-- Get count of the user's public badges
SELECT COUNT(id) AS count FROM badge WHERE visibility = 'public' AND deleted = 0 AND user_id = :user_id

-- name: select-public-badge-groups
-- Get user badge tags which are associated to public badges
-- FIXME (rename badge -> user_badge)
SELECT bt.tag AS name, MAX(bt.id) AS id, COUNT(DISTINCT b.id) AS badges FROM badge AS b
       JOIN badge_tag AS bt ON (bt.badge_id = b.id)
       WHERE b.user_id = :user_id AND b.deleted = 0 AND b.visibility = 'public'
       GROUP BY bt.tag

--name: select-badge-tag
-- Get badge tag name by id
SELECT tag FROM badge_tag WHERE id = :id
