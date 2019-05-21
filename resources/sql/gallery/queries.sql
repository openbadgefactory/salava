-- name: select-users-public-badges
SELECT ub.id, badge.id AS badge_id, bc.name, bc.description, bc.image_file, ub.issued_on, ub.expires_on, ub.visibility, ub.mtime, ub.badge_id, ub.assertion_url, ic.name AS issuer_content_name, ic.url AS issuer_content_url,
COUNT(ube.id) AS user_endorsements_count, COUNT(DISTINCT bec.endorsement_content_id) AS endorsement_count
FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
LEFT JOIN badge_endorsement_content AS bec ON (bec.badge_id = ub.badge_id)
LEFT JOIN user_badge_endorsement AS ube ON (ube.user_badge_id = ub.id) AND ube.status = 'accepted'
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
               WHERE profile_visibility = 'public' OR profile_visibility = 'internal' AND activated = 1
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
badge.default_language_code, badge.last_received,
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

-- name: select-common-badge-rating-g
SELECT AVG(rating) AS average_rating, COUNT(rating) AS rating_count FROM user_badge ub
WHERE ub.visibility = 'public' AND ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND gallery_id = :gallery_id AND (rating IS NULL OR rating > 0)

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

-- name: select-badge-recipients-fix-2
SELECT DISTINCT u.id, u.first_name, u.last_name, u.profile_picture, ub.visibility FROM user AS u
       JOIN user_badge AS ub ON ub.user_id = u.id
       JOIN badge AS badge ON (badge.id = ub.badge_id)
       JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
       JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
       JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
       JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
       WHERE bc.name = :name AND ic.name = :issuer_content_name AND status = 'accepted' AND ub.deleted = 0

-- name: select-badge-recipients-g
SELECT DISTINCT u.id, first_name, last_name, profile_picture, visibility FROM user u
INNER JOIN user_badge AS ub ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND ub.gallery_id = :gallery_id

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


--name: select-gallery-badges-order-by-recipients-old
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
--GROUP BY badge.id
ORDER BY recipients DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ic-name-old
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
--GROUP BY badge.id
ORDER BY ic.name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-name-old
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
--GROUP BY badge.id
ORDER BY bc.name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ctime-old
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
--GROUP BY badge.id
ORDER BY ctime DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-recipients
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY recipients DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ic-name
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY issuer_content_name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-name
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY badge_name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ctime
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY ctime DESC
LIMIT :limit OFFSET :offset


--name: select-gallery-tags-old
SELECT bct.tag, GROUP_CONCAT(bbc.badge_id) AS badge_ids, COUNT(bbc.badge_id) as badge_id_count
FROM badge_badge_content as bbc
JOIN badge_content_tag as bct on (bct.badge_content_id = bbc.badge_content_id)
WHERE bct.tag IN (SELECT tag FROM badge_content_tag AS bct
      	      	 JOIN badge_badge_content AS bbc ON (bct.badge_content_id = bbc.badge_content_id)
      	      	 WHERE bbc.badge_id IN  (:badge_ids))
AND bbc.badge_id IN (SELECT DISTINCT badge_id FROM badge WHERE published = 1 and recipient_count > 0)
GROUP BY bct.tag

--name: gallery-badges-count
SELECT COUNT(DISTINCT id) AS badges_count FROM badge WHERE published = 1 AND recipient_count > 0

--name: gallery-pages-count
SELECT COUNT(id) AS pages_count FROM page WHERE (visibility = 'public' OR visibility = 'internal') AND deleted = 0

--name: gallery-profiles-count
SELECT COUNT(id) AS profiles_count FROM user WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0 AND activated = 1

--name: gallery-badges-count-since-last-login
SELECT COUNT(se.id) AS badges_count FROM social_event AS se
JOIN user_badge AS ub ON se.object = ub.id
WHERE se.verb = 'publish' AND se.type = 'badge' AND se.mtime > :last_login AND ub.deleted = 0 AND ub.revoked = 0 AND subject != :user_id

--name: gallery-pages-count-since-last-login
SELECT COUNT(se.id) AS pages_count FROM social_event AS se
JOIN page AS p ON se.object = p.id
WHERE se.verb = 'publish' AND se.type = 'page' AND se.mtime > :last_login AND p.deleted = 0 AND subject != :user_id

--name: gallery-profiles-count-since-last-login
SELECT COUNT(id) AS profiles_count FROM user WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0 AND activated = 1 AND ctime > :last_login


--name: select-gallery-tags
SELECT DISTINCT t.tag FROM badge_content_tag t
INNER JOIN badge_badge_content bc ON bc.badge_content_id = t.badge_content_id
INNER JOIN user_badge ub ON bc.badge_id = ub.badge_id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
ORDER BY ub.ctime DESC
LIMIT 10000;


--name: select-gallery-ids
SELECT DISTINCT ub.gallery_id FROM user_badge ub
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-gallery-ids-country
SELECT DISTINCT ub.gallery_id FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND u.country = :country
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-gallery-ids-badge
SELECT DISTINCT ub.gallery_id FROM user_badge ub
INNER JOIN gallery g ON ub.gallery_id = g.id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND g.badge_name LIKE :badge
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-gallery-ids-issuer
SELECT DISTINCT ub.gallery_id FROM user_badge ub
INNER JOIN gallery g ON ub.gallery_id = g.id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND g.issuer_name LIKE :issuer
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-gallery-ids-recipient
SELECT DISTINCT ub.gallery_id FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND CONCAT(u.first_name, ' ', u.last_name) LIKE :recipient
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-gallery-ids-tags
SELECT DISTINCT ub.gallery_id FROM user_badge ub
INNER JOIN badge_badge_content bc ON ub.badge_id = bc.badge_id
INNER JOIN badge_content_tag t ON bc.badge_content_id = t.badge_content_id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND t.tag IN (:tags)
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-gallery-id
SELECT ub.gallery_id FROM user_badge ub
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND ub.badge_id = :badge_id
ORDER BY ub.ctime DESC
LIMIT 1;

