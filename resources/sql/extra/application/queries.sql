-- name: insert-badge-advert<!
INSERT INTO badge_advert
   (remote_url, remote_id, remote_issuer_id, info, application_url,
    application_url_label, issuer_content_id, badge_content_id,
    criteria_content_id, criteria_url, kind, country, not_before, not_after,
    ctime, mtime)
VALUES
   (:remote_url, :remote_id, :remote_issuer_id, :info, :application_url,
    :issuer_content_id, :badge_content_id, :criteria_content_id, :kind,
    :country, :not_before, :not_after, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

-- name: update-badge-advert
UPDATE badge_advert SET
    remote_url = :remote_url, remote_id = :remote_id,
    remote_issuer_id = :remote_issuer_id, info = :info,
    application_url = :application_url, application_url_label = :application_url_label,
    issuer_content_id = :issuer_content_id, badge_content_id = :badge_content_id,
    criteria_content_id = :criteria_content_id, criteria_url = :criteria_url,
    kind = :kind, country = :country, not_before = :not_before, not_after = :not_after,
    mtime = UNIX_TIMESTAMP()
WHERE id = :id

-- name: replace-badge-advert!
INSERT INTO badge_advert
   (remote_url, remote_id, remote_issuer_id, info, application_url, application_url_label,
    issuer_content_id, badge_content_id, criteria_content_id, criteria_url, kind,
    country, not_before, not_after, ctime, mtime)
VALUES
   (:remote_url, :remote_id, :remote_issuer_id, :info, :application_url, :application_url_label,
    :issuer_content_id, :badge_content_id, :criteria_content_id, :criteria_url, :kind,
    :country, :not_before, :not_after, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE
    info = :info, application_url = :application_url, application_url_label = :application_url_label,
    issuer_content_id = :issuer_content_id, badge_content_id = :badge_content_id,
    criteria_content_id = :criteria_content_id, criteria_url = :criteria_url, kind = :kind,
    country = :country, not_before = :not_before, not_after = :not_after,
    deleted = 0, mtime = UNIX_TIMESTAMP()

-- name: update-badge-adverd-deleted
UPDATE badge_advert SET deleted = :deleted WHERE id = :id

-- name: select-badge-adverts
SELECT ba.id, bc.name, ba.info, bc.image_file,
    ic.name AS issuer_content_name, ic.url AS issuer_content_url,
    GROUP_CONCAT( bct.tag) AS tags FROM badge_advert ba
    JOIN badge_badge_content AS bbc ON (bbc.badge_id = ba.badge_content_id)
     JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
INNER JOIN issuer_content AS ic ON (ic.id = ba.issuer_content_id)
LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id)
GROUP BY ba.id


--name: select-badge-advert

SELECT DISTINCT ba.id, ba.country, bc.name, bc.description, ba.criteria_url, ic.email AS issuer_contact,
       ic.image_file AS issuer_image, ba.info, bc.image_file, ic.name AS issuer_content_name,
       ic.url AS issuer_content_url,GROUP_CONCAT( bct.tag) AS tags, ba.mtime, ba.not_before,
       ba.not_after, ba.kind, ba.application_url, ba.application_url_label, IF(scba.user_id, true, false) AS followed
       FROM badge_advert AS ba
       JOIN badge_content AS bc ON (bc.id = ba.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = ba.issuer_content_id)
       LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id)
       LEFT JOIN social_connections_badge_advert AS scba ON (scba.badge_advert_id = ba.id and scba.user_id = :user_id)
       WHERE ba.deleted = 0 AND ba.id = :id

-- name: select-badge-advert-countries
SELECT country FROM badge_advert WHERE deleted=0  AND (not_before = 0 OR not_before < UNIX_TIMESTAMP()) AND (not_after = 0 OR not_after > UNIX_TIMESTAMP()) ORDER BY country  

-- name: select-user-country
SELECT country FROM user WHERE id = :id

-- name: select-badge-content-tags
SELECT bct.tag, GROUP_CONCAT(ba.badge_content_id) AS badge_content_ids, COUNT(ba.badge_content_id) as badge_content_id_count from badge_advert AS ba
       JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id)
       WHERE ba.country = :country
       GROUP BY bct.tag ORDER BY tag LIMIT 1000


-- name: select-badge-names
SELECT name FROM badge_content ORDER BY name LIMIT 1000

-- name: unpublish-badge-advert-by-remote!
UPDATE badge_advert SET deleted = 1, mtime = UNIX_TIMESTAMP()
WHERE remote_url = :remote_url AND remote_id = :remote_id AND remote_issuer_id = :remote_issuer_id



--name: insert-connect-badge-advert<!
--add new connect with badge advert
INSERT IGNORE INTO social_connections_badge_advert (user_id, badge_advert_id, ctime)
                   VALUES (:user_id, :badge_advert_id, UNIX_TIMESTAMP())
		   
--name: delete-connect-badge-advert!
DELETE FROM social_connections_badge_advert WHERE user_id = :user_id  AND badge_advert_id = :badge_advert_id
