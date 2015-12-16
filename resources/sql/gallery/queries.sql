-- name: select-users-public-badges
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, assertion_url, ic.name AS issuer_name, ic.url AS issuer_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       WHERE visibility = 'public' AND status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > unix_timestamp()) AND user_id = :user_id
       ORDER BY ctime DESC

-- name: select-badge-countries
SELECT country FROM user AS u
               LEFT JOIN badge AS b ON b.user_id = u.id
               WHERE b.visibility = "public"
               GROUP BY country
               ORDER BY country

-- name: select-user-country
SELECT country FROM user WHERE id = :id
