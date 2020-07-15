-- name: select-users-public-badges
SELECT ub.id, badge.id AS badge_id, bc.name, bc.description, bc.image_file, ub.issued_on, ub.expires_on, ub.visibility, ub.mtime, ub.badge_id, ub.assertion_url, ic.name AS issuer_content_name, ic.url AS issuer_content_url,
COUNT(ube.id) AS user_endorsement_count, COUNT(DISTINCT bec.endorsement_content_id) AS endorsement_count
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

--name: select-multi-language-badge-content-p
--get badge by id for public API
SELECT
badge.id as badge_id,
badge.default_language_code,
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

-- name: select-common-badge-rating-g
SELECT AVG(rating) AS average_rating, COUNT(rating) AS rating_count FROM user_badge ub
WHERE ub.visibility = 'public' AND ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND gallery_id = :gallery_id AND (rating IS NULL OR rating > 0)

-- name: select-users-public-pages
SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
       JOIN user AS u ON p.user_id = u.id
       LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
       WHERE user_id = :user_id AND p.deleted = 0  AND (visibility = 'public' OR visibility = :visibility)
       GROUP BY p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name
       ORDER BY p.mtime DESC
       LIMIT 100

-- name: select-badge-recipients-g
SELECT DISTINCT u.id, first_name, last_name, profile_picture, visibility FROM user u
INNER JOIN user_badge AS ub ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND ub.gallery_id = :gallery_id

-- name: select-common-badge-counts
SELECT ub.user_id, COUNT(DISTINCT ub.gallery_id) AS c
FROM user_badge AS ub
WHERE status = 'accepted' AND deleted = 0
    AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())
    AND gallery_id IN
      (SELECT gallery_id FROM user_badge WHERE user_id = :user_id AND status = 'accepted' AND deleted = 0 AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP()))
GROUP BY user_id

--name: select-gallery-badges-count
SELECT COUNT(DISTINCT ub.gallery_id) AS total
FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND ub.visibility != 'private' AND ub.gallery_id IS NOT NULL AND (:country = 'all' OR u.country = :country);

--name: select-gallery-badges-order-by-recipients-all
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
       MAX(ub.ctime) AS ctime,
       (SELECT CAST(COUNT(DISTINCT ub2.user_id) AS UNSIGNED)
        FROM user_badge ub2
        WHERE ub2.gallery_id = g.id AND ub2.status = 'accepted' AND ub2.deleted = 0 AND ub2.revoked = 0
        AND (ub2.expires_on IS NULL OR ub2.expires_on > UNIX_TIMESTAMP())) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
      AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country)
GROUP BY ub.gallery_id
ORDER BY
 CASE WHEN :order='name'  THEN badge_name END,
 CASE WHEN :order='issuer_content_name' THEN issuer_content_name END,
 CASE WHEN :order='recipients' THEN recipients END DESC,
 CASE WHEN :order='mtime' THEN MAX(ub.ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-all
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
       MAX(ub.ctime) AS ctime,
       (SELECT CAST(COUNT(DISTINCT ub2.user_id) AS UNSIGNED)
        FROM user_badge ub2
        WHERE ub2.gallery_id = g.id AND ub2.status = 'accepted' AND ub2.deleted = 0 AND ub2.revoked = 0
        AND (ub2.expires_on IS NULL OR ub2.expires_on > UNIX_TIMESTAMP())) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
      AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country)
GROUP BY ub.gallery_id
ORDER BY
 CASE WHEN :order='name'  THEN badge_name END,
 CASE WHEN :order='issuer_content_name' THEN issuer_content_name END,
 CASE WHEN :order='recipients' THEN recipients END DESC,
 CASE WHEN :order='mtime' THEN MAX(ub.ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-filtered
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
       MAX(ub.ctime) AS ctime,
       (SELECT CAST(COUNT(DISTINCT ub2.user_id) AS UNSIGNED)
        FROM user_badge ub2
        WHERE ub2.gallery_id = g.id AND ub2.status = 'accepted' AND ub2.deleted = 0 AND ub2.revoked = 0
        AND (ub2.expires_on IS NULL OR ub2.expires_on > UNIX_TIMESTAMP())) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
      AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country) AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY
  CASE WHEN :order='name'  THEN badge_name END,
  CASE WHEN :order='issuer_content_name' THEN issuer_content_name END,
  CASE WHEN :order='recipients' THEN recipients END DESC,
  CASE WHEN :order='mtime' THEN MAX(ub.ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-recipients-filtered
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country) AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY recipients DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ic-name-all
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country)
GROUP BY ub.gallery_id
ORDER BY issuer_content_name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ic-name-filtered
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
   AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country) AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY issuer_content_name
LIMIT :limit OFFSET :offset


--name: select-gallery-badges-order-by-name-all
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
    AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country)
GROUP BY ub.gallery_id
ORDER BY badge_name
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-name-filtered
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
     AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country) AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY badge_name
LIMIT :limit OFFSET :offset


--name: select-gallery-badges-order-by-ctime-all
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
     AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country)
GROUP BY ub.gallery_id
ORDER BY ctime DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-badges-order-by-ctime-filtered
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, g.issuer_name AS issuer_content_name,
    MAX(ub.ctime) AS ctime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
     AND ub.visibility != 'private' AND (:country = 'all' OR u.country = :country) AND g.id IN (:gallery_ids)
GROUP BY ub.gallery_id
ORDER BY ctime DESC
LIMIT :limit OFFSET :offset

--name: gallery-badges-count
SELECT COUNT(DISTINCT ub.gallery_id) AS badges_count FROM user_badge ub
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())

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

--name: select-gallery-ids-selfie
SELECT DISTINCT ub.gallery_id FROM user_badge ub
INNER JOIN selfie_badge sb ON ub.selfie_id = sb.id
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND ub.selfie_id IS NOT NULL AND (:country = 'all' OR u.country = :country)
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-gallery-id
SELECT ub.gallery_id FROM user_badge ub
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND ub.badge_id = :badge_id
ORDER BY ub.ctime DESC
LIMIT 1;

--name: select-gallery-ids-space
SELECT DISTINCT ub.gallery_id FROM user_badge ub
INNER JOIN user_space us ON us.user_id = ub.user_id
INNER JOIN space s ON s.id = us.space_id
WHERE ub.status = 'accepted' AND ub.visibility != 'private' AND ub.deleted = 0
    AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP()) AND ub.gallery_id IS NOT NULL
    AND s.id = :space_id
ORDER BY ub.ctime DESC
LIMIT 100000;

--name: select-page-ids-owner
SELECT DISTINCT p.id FROM page p
JOIN user AS u ON p.user_id = u.id
WHERE CONCAT(u.first_name, ' ', u.last_name) LIKE :owner
AND (p.visibility = 'public' OR p.visibility = 'internal') AND p.deleted = 0
ORDER BY p.mtime DESC
LIMIT 100000;

--name: select-page-ids-space
SELECT DISTINCT p.id FROM page p
INNER JOIN user_space us ON us.user_id = p.user_id
INNER JOIN space s ON s.id = us.space_id
WHERE s.id = :space_id AND (p.visibility = 'public' OR p.visibility = 'internal') AND p.deleted = 0
ORDER BY p.mtime DESC
LIMIT 100000;

--name: select-gallery-pages-all
SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
JOIN user AS u ON p.user_id = u.id
LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
WHERE (visibility = 'public' OR visibility = 'internal') AND p.deleted = 0 AND (:country = 'all' OR u.country= :country)
GROUP BY p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture
ORDER BY p.mtime DESC
LIMIT :limit;

--name: select-gallery-pages-filtered
SELECT p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture, GROUP_CONCAT(pb.badge_id) AS badges FROM page AS p
JOIN user AS u ON p.user_id = u.id
LEFT JOIN page_block_badge AS pb ON pb.page_id = p.id
WHERE (visibility = 'public' OR visibility = 'internal') AND p.deleted = 0 AND p.id IN (:page_ids) AND (:country = 'all' OR u.country= :country)
GROUP BY p.id, p.ctime, p.mtime, user_id, name, description, u.first_name, u.last_name, u.profile_picture
ORDER BY p.mtime DESC
LIMIT :limit;

--name: all-users-on-map-count
SELECT COUNT(id) AS users_count FROM user WHERE location_lng IS NOT NULL AND location_lat IS NOT NULL AND deleted = 0

--name: select-user-owns-badge-id
SELECT user_id FROM user_badge WHERE gallery_id = :gallery_id AND user_id = :user_id AND status != 'declined' AND deleted = 0 AND revoked = 0

--name: filtered-select-all-profile-ids-name
SELECT DISTINCT u.id FROM user u
WHERE CONCAT(first_name, ' ', last_name) LIKE :name AND deleted = 0 AND activated = 1
      AND u.id NOT IN (SELECT user_id FROM user_space WHERE space_id = :space_id)
ORDER BY ctime DESC
LIMIT 100000;

--name: select-all-profile-ids-name
SELECT DISTINCT u.id FROM user u
WHERE CONCAT(first_name, ' ', last_name) LIKE :name AND deleted = 0 AND activated = 1
ORDER BY ctime DESC
LIMIT 100000;

--name: filtered-select-all-profile-ids-email
SELECT DISTINCT u.id FROM user u
JOIN user_email ue ON u.id= ue.user_id
WHERE u.id IN (SELECT user_id FROM user_email WHERE email LIKE :email) AND u.deleted = 0 AND u.activated = 1
      AND u.id NOT IN (SELECT user_id FROM user_space WHERE space_id = :space_id)
ORDER BY u.ctime DESC
LIMIT 100000;

--name: select-all-profile-ids-email
SELECT DISTINCT u.id FROM user u
JOIN user_email ue ON u.id= ue.user_id
WHERE u.id IN (SELECT user_id FROM user_email WHERE email LIKE :email) AND u.deleted = 0 AND u.activated = 1
ORDER BY u.ctime DESC
LIMIT 100000;

--name: select-all-profile-ids-space
SELECT DISTINCT u.id FROM user u
JOIN user_space us ON u.id= us.user_id
WHERE u.deleted = 0 AND u.activated = 1
      AND u.id IN (SELECT user_id FROM user_space WHERE space_id = :space_id)
ORDER BY u.ctime DESC
LIMIT 100000;

--name: select-profiles-count
SELECT COUNT(DISTINCT u.id) AS total
FROM user u
WHERE u.deleted = 0 AND u.activated= 1 AND (:country = 'all' OR u.country = :country)
      AND u.id NOT IN (SELECT user_id FROM user_space WHERE space_id = :space_id)

--name: filtered-select-profiles-all
SELECT id, first_name, last_name, country, profile_picture, ctime
FROM user
WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0 AND activated = 1 AND (:country = 'all' OR country= :country)
      AND id NOT IN (SELECT user_id FROM user_space WHERE space_id = :space_id)
GROUP BY id
ORDER BY
 CASE WHEN :order='name'  THEN first_name END,
 CASE WHEN :order='ctime' THEN MAX(ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-profiles-all
SELECT id, first_name, last_name, country, profile_picture, ctime
FROM user
WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0 AND activated = 1 AND (:country = 'all' OR country= :country)
GROUP BY id
ORDER BY
 CASE WHEN :order='name'  THEN first_name END,
 CASE WHEN :order='ctime' THEN MAX(ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-profiles-filtered
SELECT id, first_name, last_name, country, profile_picture, ctime
FROM user
WHERE (profile_visibility = 'public' OR profile_visibility = 'internal') AND deleted = 0 AND activated = 1 AND (:country = 'all' OR country= :country) AND id IN (:ids)
GROUP BY id
ORDER BY
 CASE WHEN :order='name'  THEN first_name END,
 CASE WHEN :order='ctime' THEN MAX(ctime) END DESC
LIMIT :limit OFFSET :offset
