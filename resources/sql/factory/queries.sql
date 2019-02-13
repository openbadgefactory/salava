--name: select-uids-emails-by-emails
SELECT user_id, email FROM user_email WHERE verified = 1 AND email IN (:emails);

--name: select-primary-emails-by-uids
SELECT user_id, email FROM user_email WHERE user_id IN (:user_ids) AND primary_address = 1;

--name: insert-pending-badge-for-email!
INSERT INTO pending_factory_badge (assertion_url, email, ctime) VALUES (:assertion_url, :email, UNIX_TIMESTAMP())

--name: select-pending-badges-by-user
SELECT DISTINCT p.assertion_url, p.email FROM pending_factory_badge AS p
INNER JOIN user_email AS ue ON p.email = ue.email
LEFT JOIN user_badge ub ON (ue.user_id = ub.user_id AND p.assertion_url = ub.assertion_url AND ub.deleted = 0)
WHERE p.assertion_url IS NOT NULL AND ue.user_id = :user_id AND ue.verified = 1
  AND ub.id IS NULL;

--name: delete-duplicate-pending-badges!
DELETE FROM pending_factory_badge
WHERE email = :email AND assertion_url =
    (SELECT assertion_url FROM user_badge WHERE user_id = :user_id AND assertion_url = :assertion_url LIMIT 1)

-- --name: select-badge-updates
-- FIXME (evidence_url)
--SELECT ub.id, ub.user_id, ub.email, ub.assertion_url, ub.mtime, ube.url AS evidence_url, ub.rating FROM user_badge AS ub
--       LEFT JOIN user_badge_evidence AS ube ON (ube.user_badge_id = ub.id)
--       WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.user_id = :user_id AND ub.id = :id

--name: select-badge-updates
SELECT ub.id, ub.user_id, ub.email, ub.assertion_url, ub.mtime, ub.rating FROM user_badge AS ub
       WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.user_id = :user_id AND ub.id = :id

--name: select-user-badge-evidence
SELECT ube.url AS id, ube.name, ube.narrative, ube.description
FROM user_badge_evidence AS ube
JOIN user_badge AS ub ON ub.id = ube.user_badge_id
WHERE ube.user_badge_id = :id

--name: select-user-badge-endorsements
SELECT ube.external_id AS id, ube.endorser_id, ube.content, ub.assertion_url
FROM user_badge_endorsement AS ube
JOIN user_badge AS ub ON ub.id = ube.user_badge_id
WHERE ube.user_badge_id = :id

-- name: select-badge-by-assertion
SELECT id FROM user_badge WHERE email = :email AND assertion_url = :url AND deleted = 0 AND status != 'declined'

--name: delete-pending-user-badge!
DELETE FROM user_badge WHERE id = :id AND user_id = 0 AND status = 'pending';
