-- name: select-verified-email-address
-- Get user id by email address
SELECT user_id FROM user_email WHERE email = :email AND verified = 1

-- name: select-public-badge-count
-- Get count of the user's public badges
SELECT COUNT(id) AS count FROM user_badge WHERE visibility = 'public' AND deleted = 0 AND user_id = :user_id

-- name: select-public-badge-groups
-- Get user badge tags which are associated to public badges
SELECT bt.tag AS name, MAX(bt.id) AS id, COUNT(DISTINCT ub.id) AS badges FROM user_badge AS ub
JOIN badge_tag AS bt ON bt.user_badge_id = ub.id
WHERE ub.user_id = :user_id AND ub.deleted = 0 AND ub.visibility = 'public'
GROUP BY bt.tag

--name: select-badge-tag
-- Get badge tag name by id
SELECT tag FROM badge_tag WHERE id = :id

-- name: select-all-user-badges
-- Get all user's public badges for diplayer
SELECT ub.id, ub.assertion_url, ub.assertion_json, ub.issued_on, ub.expires_on,
    ub.last_checked, ub.ctime, cc.url AS criteria_url, GROUP_CONCAT(e.url) AS evidence_url,
    bc.name, bc.description, bc.image_file, ic.name AS issuer_name,
    ic.url AS issuer_url, ic.email AS issuer_email, ic.description AS
    issuer_description, ic.image_file AS issuer_image
FROM user_badge ub
INNER JOIN badge b ON ub.badge_id = b.id
INNER JOIN badge_badge_content bbc ON b.id = bbc.badge_id
INNER JOIN badge_content bc ON (bbc.badge_content_id = bc.id AND bc.language_code = b.default_language_code)
INNER JOIN badge_issuer_content bic ON b.id = bic.badge_id
INNER JOIN issuer_content ic ON (bic.issuer_content_id = ic.id AND ic.language_code = b.default_language_code)
INNER JOIN badge_criteria_content bcc ON b.id = bcc.badge_id
INNER JOIN criteria_content cc ON (bcc.criteria_content_id = cc.id AND cc.language_code = b.default_language_code)
LEFT JOIN user_badge_evidence e ON ub.id = e.user_badge_id
WHERE ub.user_id = :user_id AND ub.visibility = 'public' AND ub.deleted = 0
GROUP BY ub.id

-- name: select-user-badges-with-tag
-- Get user's public badges in one tag group for diplayer
SELECT ub.id, ub.assertion_url, ub.assertion_json, ub.issued_on, ub.expires_on,
    ub.last_checked, ub.ctime, cc.url AS criteria_url, GROUP_CONCAT(e.url) AS evidence_url,
    bc.name, bc.description, bc.image_file, ic.name AS issuer_name,
    ic.url AS issuer_url, ic.email AS issuer_email, ic.description AS
    issuer_description, ic.image_file AS issuer_image
FROM user_badge ub
INNER JOIN badge_tag AS bt ON ub.id = bt.user_badge_id
INNER JOIN badge b ON ub.badge_id = b.id
INNER JOIN badge_badge_content bbc ON b.id = bbc.badge_id
INNER JOIN badge_content bc ON (bbc.badge_content_id = bc.id AND bc.language_code = b.default_language_code)
INNER JOIN badge_issuer_content bic ON b.id = bic.badge_id
INNER JOIN issuer_content ic ON (bic.issuer_content_id = ic.id AND ic.language_code = b.default_language_code)
INNER JOIN badge_criteria_content bcc ON b.id = bcc.badge_id
INNER JOIN criteria_content cc ON (bcc.criteria_content_id = cc.id AND cc.language_code = b.default_language_code)
LEFT JOIN user_badge_evidence e ON ub.id = e.user_badge_id
WHERE ub.user_id = :user_id AND bt.tag = :tag AND ub.visibility = 'public' AND ub.deleted = 0
GROUP BY ub.id
