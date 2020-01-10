--name: select-user-badge-evidence
SELECT ube.id, ube.url, ube.description, ube.narrative, ube.name, ube.ctime, ube.mtime
FROM user_badge_evidence AS ube
JOIN user_badge AS ub ON ub.id = ube.user_badge_id
WHERE ube.user_badge_id = :user_badge_id

--name: insert-evidence<!
INSERT INTO user_badge_evidence (user_badge_id, url, name, description, narrative, ctime, mtime )
VALUES (:user_badge_id, :url, :name, :description, :narrative, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: update-user-badge-evidence!
UPDATE user_badge_evidence SET url = :url, name = :name,
narrative = :narrative, mtime = UNIX_TIMESTAMP()
WHERE id = :id AND user_badge_id = :user_badge_id

--name: delete-user-badge-evidence!
DELETE FROM user_badge_evidence WHERE id = :id AND user_badge_id = :user_badge_id

-- name: insert-user-evidence-property!
-- this is used to more information about evidences e.g. {:hidden true :type url}
REPLACE INTO user_properties (user_id, name, value)
 VALUES (:user_id, :name, :value)

--name: delete-user-evidence-property!
DELETE FROM user_properties WHERE name = :name AND user_id = :user_id

--name: select-user-evidence-property
SELECT value FROM user_properties WHERE name = :name AND user_id = :user_id

--name: select-user-evidence-by-url
SELECT DISTINCT ube.url FROM user_badge_evidence AS ube
JOIN user_badge AS ub ON ub.id = ube.user_badge_id
WHERE ub.user_id = :user_id AND ube.url = :url

--name: delete-all-badge-evidences!
DELETE FROM user_badge_evidence WHERE user_badge_id = :user_badge_id
