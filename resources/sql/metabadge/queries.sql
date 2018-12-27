--name: select-user-badge-by-assertion-url
SELECT ub.id AS user_badge_id, bc.name, bc.image_file, ub.issued_on, ub.status, ub.deleted FROM user_badge AS ub
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
WHERE ub.assertion_url = :assertion_url AND ub.revoked = 0
ORDER BY ctime DESC

--name: select-completed-metabadges
SELECT ubm.meta_badge AS metabadge_id, ubm.user_badge_id, fm.min_required, fm.name, bc.description, bc.image_file, cc.markdown_text AS criteria_content
FROM user_badge_metabadge AS ubm
JOIN user_badge AS ub ON (ubm.user_badge_id = ub.id)
JOIN factory_metabadge AS fm ON (fm.id = ubm.meta_badge)
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id AND cc.language_code = badge.default_language_code)
JOIN user as u on (u.id = ub.user_id)
WHERE u.id = :user_id
GROUP BY fm.id;

--name: select-received-required-badges
SELECT fmr.metabadge_id, fmr.required_badge_id, ubm.user_badge_id, bc.name, bc.image_file, ub.issued_on, ub.status, ub.deleted
FROM user_badge_metabadge AS ubm
JOIN user_badge AS ub ON (ubm.user_badge_id = ub.id)
JOIN factory_metabadge_required AS fmr ON (ubm.meta_badge_req = fmr.required_badge_id)
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
JOIN user as u on (u.id = ub.user_id)
WHERE fmr.metabadge_id = :metabadge_id AND u.id = :user_id
GROUP BY ub.id
ORDER BY ub.issued_on ASC

--name: select-all-required-badges
SELECT metabadge_id, required_badge_id, name, description, criteria AS criteria_content, image_file
FROM factory_metabadge_required
WHERE metabadge_id = :metabadge_id;

--name: select-user-required-badges
SELECT ub.id, ubm.meta_badge_req from user_badge AS ub
JOIN user_badge_metabadge as ubm on (ubm.user_badge_id = ub.id)
JOIN user as u on (u.id = ub.user_id)
where u.id = :user_id
GROUP BY ubm.meta_badge_req

--name: select-all-user-metabadges
SELECT fm.id AS metabadge_id, fm.min_required, fm.name, fm.description, fm.image_file, fm.criteria AS criteria_content
FROM user_badge as ub
JOIN user_badge_metabadge as ubm on (ubm.user_badge_id = ub.id)
JOIN factory_metabadge_required as fmr on (fmr.required_badge_id = ubm.meta_badge_req)
JOIN factory_metabadge as fm on (fm.id = fmr.metabadge_id)
JOIN user as u on (u.id = ub.user_id)
WHERE u.id = :user_id
GROUP BY fm.id

--name: select-metabadge-info-from-user-badge
--check if user badge is a milestone badge or a required badge
SELECT meta_badge, meta_badge_req FROM user_badge_metabadge AS ubm WHERE ubm.user_badge_id = :id

--name: select-completed-metabadge-by-badge-id
SELECT ubm.meta_badge AS metabadge_id, ubm.user_badge_id, fm.min_required, fm.name, bc.description, bc.image_file, cc.markdown_text AS criteria_content
FROM user_badge_metabadge AS ubm
JOIN user_badge AS ub ON (ubm.user_badge_id = ub.id)
JOIN factory_metabadge AS fm ON (fm.id = ubm.meta_badge)
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id AND cc.language_code = badge.default_language_code)
JOIN user as u on (u.id = ub.user_id)
WHERE u.id = :user_id AND ubm.user_badge_id = :user_badge_id
GROUP BY fm.id;

--name: select-required-metatabadge-by-badge-id
SELECT fmr.metabadge_id FROM user_badge_metabadge AS ubm
JOIN user_badge AS ub ON (ubm.user_badge_id = ub.id)
JOIN factory_metabadge_required AS fmr ON (ubm.meta_badge_req = fmr.required_badge_id)
WHERE ubm.user_badge_id = :id

--name: select-received-metabadge-by-badge-id
SELECT ub.id, ub.user_id, ub.assertion_url, ubm.last_modified FROM user_badge_metabadge AS ubm
JOIN user_badge AS ub ON ubm.user_badge_id = ub.id
where ubm.user_badge_id = :id

--name: select-user-received-metabadge-by-metabadge-id
--check if user has received metabadge
SELECT meta_badge AS metabadge_id, user_badge_id FROM user_badge_metabadge AS ubm
JOIN user_badge AS ub ON (ub.id = ubm.user_badge_id)
WHERE ubm.meta_badge = :metabadge_id AND ub.user_id = :user_id

--name: select-completed-metabadge-by-metabadge-id
SELECT ubm.meta_badge AS metabadge_id, ubm.user_badge_id, fm.min_required, fm.name, bc.description, bc.image_file, cc.markdown_text AS criteria_content
FROM user_badge_metabadge AS ubm
JOIN factory_metabadge AS fm ON (fm.id = ubm.meta_badge)
JOIN user_badge AS ub ON (ubm.user_badge_id = ub.id)
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id AND cc.language_code = badge.default_language_code)
JOIN user as u on (u.id = ub.user_id)
WHERE u.id = :user_id AND ubm.meta_badge = :metabadge_id
GROUP BY fm.id

--name: select-factory-metabadge
--select default factory metabadge info
SELECT id AS metabadge_id, name, description, image_file, criteria AS criteria_content, min_required
FROM factory_metabadge WHERE id = :id

--name: select-user-id-by-user-badge-id
SELECT user_id from user_badge AS ub where ub.id = :id

--name: select-factory-metabadge-required
SELECT name, description, criteria AS criteria_content, image_file
FROM factory_metabadge_required WHERE metabadge_id = :metabadge_id AND required_badge_id = :required_badge_id

--name: select-all-badges
--fix
SELECT id, assertion_url FROM user_badge
WHERE assertion_url IS NOT NULL AND assertion_url LIKE :obf_url
AND revoked = 0 AND status != 'declined'
AND (expires_on IS NULL OR expires_on > UNIX_TIMESTAMP())
ORDER BY last_checked LIMIT 1000

--name: select-all-metabadges
SELECT DISTINCT ub.id, ub.user_id, ub.assertion_url, ubm.last_modified FROM user_badge_metabadge AS ubm
JOIN user_badge AS ub ON ubm.user_badge_id = ub.id
WHERE ubm.meta_badge IS NOT NULL OR ubm.meta_badge_req IS NOT NULL

--name: insert-user-badge-metabadge!
INSERT IGNORE INTO user_badge_metabadge (user_badge_id, meta_badge, meta_badge_req, last_modified)
VALUES (:user_badge_id, :meta_badge, :meta_badge_req, :last_modified)

--name: delete-user-badge-metabadge!
DELETE FROM user_badge_metabadge WHERE user_badge_id = :user_badge_id

--name: insert-factory-metabadge!
INSERT IGNORE INTO factory_metabadge (id, name, description, criteria, image_file, min_required, ctime, mtime)
VALUES (:id, :name, :description, :criteria, :image_file, :min_required, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: delete-factory-metabadge!
DELETE fm,fmr FROM factory_metabadge AS fm
JOIN factory_metabadge_required AS fmr ON fmr.metabadge_id = fm.id
WHERE fm.id = :id;

--name: insert-factory-metabadge-required-badge!
INSERT IGNORE INTO factory_metabadge_required (metabadge_id, required_badge_id, name, description, criteria, image_file, ctime, mtime)
VALUES (:metabadge_id, :required_badge_id, :name, :description, :criteria, :image_file, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())


