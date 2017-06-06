-- name: select-users-public-badges
-- FIXME (content columns)
SELECT badge.id, bc.name, bc.description, bc.image_file, issued_on, expires_on, visibility, mtime, badge_content_id, assertion_url, ic.name AS issuer_content_name, ic.url AS issuer_content_url FROM badge
       JOIN badge_content AS bc ON (bc.id = badge.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = badge.issuer_content_id)
       WHERE (visibility = 'public' OR visibility = :visibility) AND status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > unix_timestamp()) AND user_id = :user_id
       ORDER BY ctime DESC

-- name: select-badge-countries
-- FIXME (rename badge -> user_badge)
SELECT country FROM user AS u
               LEFT JOIN badge AS b ON b.user_id = u.id
               WHERE b.visibility = 'public' OR b.visibility = 'internal'
               GROUP BY country
               ORDER BY country

-- name: select-page-countries
SELECT country FROM user AS u
               LEFT JOIN page AS p ON p.user_id = u.id
               WHERE p.visibility = 'public' OR p.visibility = 'internal'
               GROUP BY country
               ORDER BY country

--name: select-profile-countries
SELECT country FROM user AS u
               WHERE profile_visibility = 'public'
               GROUP BY country
               ORDER BY country

-- name: select-user-country
SELECT country FROM user WHERE id = :id

-- name: select-common-badge-content
SELECT bc.name, bc.description, bc.image_file, bc.id AS badge_content_id, GROUP_CONCAT( bct.tag) AS tags FROM badge AS badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = bc.id)
WHERE badge.id = :id
GROUP BY badge.id

-- name: select-common-badge-rating
SELECT AVG(rating) AS average_rating, COUNT(rating) AS rating_count FROM user_badge AS ub
       JOIN badge AS badge ON (badge.id = ub.badge_id)
       WHERE ub.visibility = 'public' AND ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND badge.id = :badge_content_id AND (rating IS NULL OR rating > 0)

-- name: select-badge-criteria-issuer-by-recipient
-- FIXME (badge_url? rename badge -> user_badge, use new badge table)
SELECT
issuer_verified, last_received AS ctime,
ic.url AS issuer_content_url,
ic.email AS issuer_contact,
ic.image_file AS issuer_image,
cc.markdown_text AS criteria_content,
cc.url AS criteria_url,
crc.name AS creator_name, crc.url AS creator_url,
crc.email AS creator_email,
crc.image_file AS creator_image
FROM badge
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id) AND cc.language_code = badge.default_language_code
JOIN badge_creator_content AS bcrc ON (bcrc.badge_id = badge.id)
JOIN creator_content AS crc ON (crc.id = bcrc.creator_content_id)  AND crc.language_code = badge.default_language_code
JOIN user_badge AS ub ON (badge.id = ub.badge_id)
WHERE ub.user_id = :user_id AND badge.id = :badge_content_id
ORDER By ctime DESC
LIMIT 1

-- name: select-badge-criteria-issuer-by-date
-- FIXME (badge_url? rename badge -> user_badge, use new badge table)
SELECT
issuer_verified, last_received AS ctime,
ic.url AS issuer_content_url,
ic.email AS issuer_contact,
ic.image_file AS issuer_image,
cc.markdown_text AS criteria_content,
cc.url AS criteria_url,
crc.name AS creator_name, crc.url AS creator_url,
crc.email AS creator_email,
crc.image_file AS creator_image
FROM badge
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id) AND cc.language_code = badge.default_language_code
JOIN badge_creator_content AS bcrc ON (bcrc.badge_id = badge.id)
JOIN creator_content AS crc ON (crc.id = bcrc.creator_content_id)  AND crc.language_code = badge.default_language_code
WHERE badge.id = :badge_content_id
ORDER By ctime DESC
LIMIT 1

-- name: select-users-public-pages
SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
       JOIN user AS u ON p.user_id = u.id
       LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
       WHERE user_id = :user_id AND p.deleted = 0  AND (visibility = 'public' OR visibility = :visibility) 
       GROUP BY p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name
       ORDER BY p.mtime DESC
       LIMIT 100

-- name: select-badge-recipients
SELECT DISTINCT u.id, first_name, last_name, profile_picture, visibility FROM user AS u
       JOIN user_badge AS ub ON ub.user_id = u.id
       JOIN badge AS badge ON (badge.id = ub.badge_id)
       WHERE badge.id = :badge_content_id AND status = 'accepted' AND ub.deleted = 0

-- name: select-common-badge-counts
SELECT ub.user_id,
       COUNT(DISTINCT ub.badge_id) AS c
       FROM user_badge AS ub
       WHERE status = 'accepted' AND deleted = 0
       AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())
       AND badge_id IN (SELECT DISTINCT badge_id  FROM user_badge  WHERE user_id = :user_id  AND status = 'accepted' AND deleted = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP()))
       AND user_id IN (:user_ids)
       GROUP BY user_id

-- name: select-badges-recipients
SELECT badge_id, count(distinct user_id) as recipients FROM user_badge 
       WHERE badge_id IN (:badge_ids) AND status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > unix_timestamp())
       GROUP BY badge_id


--name: select-gallery-badges-order-by-recipients
-- FIXME (content columns)
SELECT
bc.name, bc.image_file,
badge.last_received AS ctime,
badge.recipient_count AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY badge.id
ORDER BY recipients DESC
LIMIT :limit OFFSET :offset


--name: select-gallery-badges-order-by-ic-name
-- FIXME (content columns)
SELECT
bc.name, bc.image_file,
badge.last_received AS ctime,
badge.recipient_count AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY badge.id
ORDER BY ic.name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-name
-- FIXME (content columns)
SELECT
bc.name, bc.image_file,
badge.last_received AS ctime,
badge.recipient_count AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY badge.id
ORDER BY bc.name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ctime
-- FIXME (content columns)
SELECT
bc.name, bc.image_file,
badge.last_received AS ctime,
badge.recipient_count AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = ub.badge_id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = ub.badge_id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY badge.id
ORDER BY ctime DESC
LIMIT :limit OFFSET :offset


--name: select-gallery-tags
-- FIXME
SELECT bct.tag, GROUP_CONCAT(bct.badge_content_id) AS badge_content_ids, COUNT(bct.badge_content_id) as badge_content_id_count 
FROM badge_content_tag AS bct 
WHERE bct.tag IN
	(SELECT tag FROM badge_content_tag WHERE badge_content_id IN (:badge_content_ids))
	AND bct.badge_content_id IN (SELECT DISTINCT badge_content_id FROM badge WHERE visibility != 'private' AND  status = 'accepted' AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP()))
GROUP BY bct.tag
