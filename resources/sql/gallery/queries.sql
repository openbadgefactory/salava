-- name: select-users-public-badges
SELECT ub.id, badge.id AS badge_id, bc.name, bc.description, bc.image_file, ub.issued_on, ub.expires_on, ub.visibility, ub.mtime, ub.badge_id, ub.assertion_url, ic.name AS issuer_content_name, ic.url AS issuer_content_url
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE (ub.visibility = 'public' OR ub.visibility = :visibility) AND ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > unix_timestamp()) AND ub.user_id = :user_id
GROUP BY ub.id
ORDER BY ub.ctime DESC


-- name: select-badge-countries
SELECT country FROM user AS u
               LEFT JOIN user_badge AS ub ON ub.user_id = u.id
               WHERE ub.visibility = 'public' OR ub.visibility = 'internal'
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
SELECT bc.name, bc.description, bc.image_file, badge.id AS badge_id, GROUP_CONCAT( bct.tag) AS tags FROM badge AS badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = bc.id)
WHERE badge.id = :id
GROUP BY badge.id




--name: select-multi-language-badge-content
--get badge by id
SELECT
badge.id as badge_id, badge.remote_url, badge.issuer_verified,
badge.default_language_code,
bbc.badge_content_id,
bc.language_code,
bc.name, bc.description,
bc.image_file,
ic.id AS issuer_content_id,
ic.name AS issuer_content_name,
ic.url AS issuer_content_url,
ic.description AS issuer_description,
ic.email AS issuer_contact,
ic.image_file AS issuer_image,
crc.id AS creator_content_id,
crc.name AS creator_name, crc.url AS creator_url,
crc.email AS creator_email,
crc.image_file AS creator_image,
crc.description AS creator_description,
cc.markdown_text AS criteria_content,
cc.url AS criteria_url,
COUNT(DISTINCT bec.endorsement_content_id) AS endorsement_count
FROM badge AS badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id)
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id)
LEFT JOIN badge_creator_content AS bcrc ON (bcrc.badge_id = badge.id)
LEFT JOIN creator_content AS crc ON (crc.id = bcrc.creator_content_id)
LEFT JOIN badge_endorsement_content AS bec ON badge.id = bec.badge_id
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id) AND bc.language_code = cc.language_code AND ic.language_code = cc.language_code
WHERE badge.id = :id
GROUP BY bc.language_code, cc.language_code, ic.language_code


-- name: select-common-badge-rating
SELECT AVG(rating) AS average_rating, COUNT(rating) AS rating_count FROM user_badge AS ub
       JOIN badge AS badge ON (badge.id = ub.badge_id)
       WHERE ub.visibility = 'public' AND ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND badge.id = :badge_id AND (rating IS NULL OR rating > 0)

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
LEFT JOIN badge_creator_content AS bcrc ON (bcrc.badge_id = badge.id)
LEFT JOIN creator_content AS crc ON (crc.id = bcrc.creator_content_id)  AND crc.language_code = badge.default_language_code
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
       WHERE badge.id = :badge_id AND status = 'accepted' AND ub.deleted = 0

-- name: select-badge-recipients-fix
SELECT DISTINCT u.id, u.first_name, u.last_name, u.profile_picture, ub.visibility FROM user AS u
       JOIN user_badge AS ub ON ub.user_id = u.id
       JOIN badge AS badge ON (badge.id = ub.badge_id)
       JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
       JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
       JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
       JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
       WHERE bc.name = :name AND ic.name = :issuer_content_name AND bc.description = :description AND status = 'accepted' AND ub.deleted = 0

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
badge.id AS badge_id, bc.name, bc.image_file,
badge.last_received AS ctime,
CAST(SUM(badge.recipient_count) AS UNSIGNED) AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY ic.name, bc.name
ORDER BY recipients DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ic-name
-- FIXME GROUP BY ic.name, bc.name instead badge.id
SELECT
badge.id AS badge_id, bc.name, bc.image_file,
badge.last_received AS ctime,
CAST(SUM(badge.recipient_count) AS UNSIGNED) AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY ic.name, bc.name
ORDER BY ic.name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-name
-- FIXME GROUP BY ic.name, bc.name instead badge.id
SELECT
badge.id AS badge_id, bc.name, bc.image_file,
badge.last_received AS ctime,
CAST(SUM(badge.recipient_count) AS UNSIGNED) AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY ic.name, bc.name
ORDER BY bc.name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ctime
-- FIXME GROUP BY ic.name, bc.name instead badge.id
SELECT
badge.id AS badge_id, bc.name, bc.image_file,
badge.last_received AS ctime,
CAST(SUM(badge.recipient_count) AS UNSIGNED) AS recipients,
ic.name AS issuer_content_name
FROM badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE badge.id IN (:badge_ids)
GROUP BY badge.id
ORDER BY ctime DESC
LIMIT :limit OFFSET :offset


--name: select-gallery-tags
SELECT bct.tag, GROUP_CONCAT(bbc.badge_id) AS badge_ids, COUNT(bbc.badge_id) as badge_id_count
FROM badge_badge_content as bbc
JOIN badge_content_tag as bct on (bct.badge_content_id = bbc.badge_content_id)
WHERE bct.tag IN (SELECT tag FROM badge_content_tag AS bct
      	      	 JOIN badge_badge_content AS bbc ON (bct.badge_content_id = bbc.badge_content_id)
      	      	 WHERE bbc.badge_id IN  (:badge_ids))
AND bbc.badge_id IN (SELECT DISTINCT badge_id FROM badge WHERE published = 1 and recipient_count > 0)
GROUP BY bct.tag
