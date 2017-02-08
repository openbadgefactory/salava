--name: insert-badge-advert<!
INSERT INTO badge_advert (remote_url, remote_id, remote_issuer_id, info, application_url, issuer_content_id, badge_content_id, criteria_content_id, kind, country, not_before, not_after, ctime, mtime) VALUES (:remote_url, :remote_id, :remote_issuer_id, :info, :application_url, :issuer_content_id, :badge_content_id, :criteria_content_id, :kind, :country, :not_before, :not_after, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

--name: update-badge-advert
UPDATE badge_advert SET remote_url = :remote_url, remote_id = :remote_id, remote_issuer_id = :remote_issuer_id, info = :info, application_url = :application_url, issuer_content_id = :issuer_content_id, badge_content_id = :badge_content_id, criteria_content_id = :criteria_content_id, kind = :kind, country = :country, not_before = :not_before, not_after = :not_after,  mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-badge-adverd-deleted
UPDATE badge_advert SET deleted = :deleted WHERE id = :id

--name: select-badge-adverts
SELECT DISTINCT ba.id, bc.name, ba.info, bc.image_file, ic.name AS issuer_content_name, ic.url AS issuer_content_url,GROUP_CONCAT( bct.tag) AS tags FROM badge_advert AS ba
       JOIN badge_content AS bc ON (bc.id = ba.badge_content_id)
       JOIN issuer_content AS ic ON (ic.id = ba.issuer_content_id)
       LEFT JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id)
       GROUP BY ba.id


-- name: select-badge-advert-countries
SELECT country FROM badge_advert
       	       ORDER BY country

-- name: select-user-country
SELECT country FROM user WHERE id = :id

-- name: select-badge-content-tags
SELECT bct.tag, GROUP_CONCAT(ba.badge_content_id) AS badge_content_ids, COUNT(ba.badge_content_id) as badge_content_id_count from badge_advert AS ba
       JOIN badge_content_tag AS bct ON (bct.badge_content_id = ba.badge_content_id)
       WHERE ba.country = :country
       GROUP BY bct.tag ORDER BY tag LIMIT 1000


--name: select-badge-names
SELECT name FROM badge_content ORDER BY name LIMIT 1000
