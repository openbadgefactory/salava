-- name: select-users-public-badges
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, assertion_url, ic.name AS issuer_content_name, ic.url AS issuer_content_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       WHERE (visibility = 'public' OR visibility = :visibility) AND status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > unix_timestamp()) AND user_id = :user_id
       ORDER BY ctime DESC

-- name: select-badge-countries
SELECT country FROM user AS u
               LEFT JOIN badge AS b ON b.user_id = u.id
               WHERE b.visibility = "public" OR b.visibility = "internal"
               GROUP BY country
               ORDER BY country

-- name: select-page-countries
SELECT country FROM user AS u
               LEFT JOIN page AS p ON p.user_id = u.id
               WHERE p.visibility = "public" OR b.visibility = "internal"
               GROUP BY country
               ORDER BY country

--name: select-profile-countries
SELECT country FROM user AS u
               WHERE profile_visibility = "public"
               GROUP BY country
               ORDER BY country

-- name: select-user-country
SELECT country FROM user WHERE id = :id

-- name: select-common-badge-content
SELECT name, description, image_file FROM badge_content AS bc WHERE bc.id = :id

-- name: select-common-badge-rating-and-recipient
SELECT AVG(rating) AS average_rating, COUNT(rating) AS rating_count, COUNT(DISTINCT user_id) AS recipient FROM badge AS b
       JOIN badge_content AS bc ON b.badge_content_id = bc.id
       WHERE b.visibility = 'public' AND b.status = 'accepted' AND b.deleted = 0 AND b.revoked = 0 AND bc.id = :badge_content_id

-- name: select-badge-criteria-issuer-by-recipient
SELECT badge_url, issuer_verified, html_content, criteria_url, ic.name AS issuer_content_name, url AS issuer_content_url, ic.email AS issuer_contact, ic.image_file AS issuer_image FROM badge AS b
       LEFT JOIN criteria_content AS cc ON cc.id = b.criteria_content_id
       LEFT JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
       WHERE user_id = :user_id AND badge_content_id = :badge_content_id
       ORDER By ctime DESC
       LIMIT 1

-- name: select-badge-criteria-issuer-by-date
SELECT badge_url, issuer_verified, html_content, criteria_url, ic.name AS issuer_content_name, url AS issuer_content_url, ic.email AS issuer_contact, ic.image_file AS issuer_image FROM badge AS b
       LEFT JOIN criteria_content AS cc ON cc.id = b.criteria_content_id
       LEFT JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
       WHERE badge_content_id = :badge_content_id
       ORDER By ctime DESC
       LIMIT 1

-- name: select-users-public-pages
SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
       JOIN user AS u ON p.user_id = u.id
       LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
       WHERE user_id = :user_id AND (visibility = 'public' OR visibility = :visibility)
       GROUP BY p.id
       ORDER BY p.mtime DESC
       LIMIT 100

-- name: select-badge-recipients
SELECT u.id, first_name, last_name, profile_picture, visibility FROM user AS u
       JOIN badge AS b ON b.user_id = u.id
       WHERE badge_content_id = :badge_content_id AND status = 'accepted' AND b.deleted = 0