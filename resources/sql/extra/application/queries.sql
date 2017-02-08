-- name: insert-badge-advert<!
INSERT INTO badge_advert
   (remote_url, remote_id, remote_issuer_id, info, application_url,
    issuer_content_id, badge_content_id, criteria_content_id, kind,
    country, not_before, not_after, ctime, mtime)
VALUES
   (:remote_url, :remote_id, :remote_issuer_id, :info, :application_url,
    :issuer_content_id, :badge_content_id, :criteria_content_id, :kind,
    :country, :not_before, :not_after, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

-- name: update-badge-advert
UPDATE badge_advert SET
    remote_url = :remote_url, remote_id = :remote_id,
    remote_issuer_id = :remote_issuer_id, info = :info,
    application_url = :application_url, issuer_content_id = :issuer_content_id,
    badge_content_id = :badge_content_id, criteria_content_id = :criteria_content_id,
    kind = :kind, country = :country, not_before = :not_before, not_after = :not_after,
    mtime = UNIX_TIMESTAMP()
WHERE id = :id

-- name: replace-badge-advert!
INSERT INTO badge_advert
   (remote_url, remote_id, remote_issuer_id, info, application_url,
    issuer_content_id, badge_content_id, criteria_content_id, kind,
    country, not_before, not_after, ctime, mtime)
VALUES
   (:remote_url, :remote_id, :remote_issuer_id, :info, :application_url,
    :issuer_content_id, :badge_content_id, :criteria_content_id, :kind,
    :country, :not_before, :not_after, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE
    info = :info, application_url = :application_url,
    issuer_content_id = :issuer_content_id, badge_content_id = :badge_content_id,
    criteria_content_id = :criteria_content_id, kind = :kind,
    country = :country, not_before = :not_before, not_after = :not_after,
    deleted = 0, mtime = UNIX_TIMESTAMP()

-- name: update-badge-adverd-deleted
UPDATE badge_advert SET deleted = :deleted WHERE id = :id

-- name: select-badge-adverts
SELECT ba.id, bc.name, ba.info, bc.image_file,
    ic.name AS issuer_content_name, ic.url AS issuer_content_url,
    GROUP_CONCAT( bct.tag) AS tags FROM badge_advert ba
INNER JOIN badge_content AS bc ON (bc.id = ba.badge_content_id)
INNER JOIN issuer_content AS ic ON (ic.id = ba.issuer_content_id)
LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id)
GROUP BY ba.id

-- name: select-badge-advert-countries
SELECT country FROM badge_advert ORDER BY country

-- name: select-user-country
SELECT country FROM user WHERE id = :id

-- name: select-badge-content-tags
SELECT tag, GROUP_CONCAT(badge_content_id) AS badge_content_ids FROM badge_content_tag
GROUP BY tag ORDER BY tag LIMIT 1000

-- name: select-badge-names
SELECT name FROM badge_content ORDER BY name LIMIT 1000

-- name: unpublish-badge-advert-by-remote!
UPDATE badge_advert SET deleted = 1, mtime = UNIX_TIMESTAMP()
WHERE remote_url = :remote_url AND remote_id = :remote_id AND remote_issuer_id = :remote_issuer_id
