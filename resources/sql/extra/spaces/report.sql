--name: select-users-for-report
SELECT u.id, u.profile_picture, u.profile_visibility, u.ctime, CONCAT(u.first_name, ' ', u.last_name) AS name, u.activated, GROUP_CONCAT(DISTINCT ue.email) AS emailaddresses,
CAST(COUNT(DISTINCT ub.id) AS UNSIGNED) AS badgecount,
(SELECT COUNT(DISTINCT id) FROM user_badge WHERE user_id = u.id AND deleted = 0 AND revoked = 0 AND visibility != 'private' AND (expires_on IS NULL OR expires_on > unix_timestamp())) AS sharedbadges,
FROM user u
LEFT JOIN user_badge ub ON ub.user_id = u.id
INNER JOIN user_email ue ON ue.user_id = u.id
WHERE u.id IN (:ids)
GROUP BY u.id

--name: select-users-for-report-fix
SELECT u.id, u.profile_picture, u.profile_visibility, u.ctime, CONCAT(u.first_name, ' ', u.last_name) AS name, u.activated, GROUP_CONCAT(DISTINCT ue.email) AS emailaddresses
FROM user u
INNER JOIN user_email ue ON ue.user_id = u.id
WHERE u.id IN (:ids)
GROUP BY u.id

--name: select-users-for-report-limit
SELECT u.id, u.profile_picture, u.profile_visibility, u.ctime, CONCAT(u.first_name, ' ', u.last_name) AS name, u.activated,  GROUP_CONCAT(DISTINCT ue.email) AS emailaddresses,
CAST(COUNT(DISTINCT ub.id) AS UNSIGNED) AS badgecount,
(SELECT COUNT(DISTINCT id) FROM user_badge WHERE user_id = u.id AND deleted = 0 AND revoked = 0 AND visibility != 'private' AND (expires_on IS NULL OR expires_on > unix_timestamp())) AS sharedbadges
FROM user u
LEFT JOIN user_badge ub ON ub.user_id = u.id
INNER JOIN user_email ue ON ue.user_id = u.id
WHERE u.id IN (:ids)
GROUP BY u.id
LIMIT :limit OFFSET :offset

--name: select-users-for-report-limit-fix
SELECT u.id, u.profile_picture, u.profile_visibility, u.ctime, CONCAT(u.first_name, ' ', u.last_name) AS name, u.activated,  GROUP_CONCAT(DISTINCT ue.email) AS emailaddresses
FROM user u
INNER JOIN user_email ue ON ue.user_id = u.id
WHERE u.id IN (:ids)
GROUP BY u.id
LIMIT :limit OFFSET :offset

--name: select-user-ids-badge
SELECT DISTINCT ub.user_id, (SELECT COUNT(DISTINCT gallery_id) FROM user_badge WHERE gallery_id IN (:badge_ids) AND user_id = ub.user_id) AS count FROM user_badge ub
INNER JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.gallery_id IN (:badge_ids) AND ub.issued_on BETWEEN IFNULL(:from, (SELECT MIN(issued_on) FROM user_badge) ) AND IFNULL(:to, (SELECT MAX(issued_on) FROM user_badge))
      AND ub.deleted = 0 AND ub.revoked = 0 AND us.space_id = :space_id
GROUP BY ub.user_id, count
HAVING count = :expected_count
ORDER BY ub.issued_on DESC
LIMIT 100000

--name: select-badge-ids-report
SELECT DISTINCT ub.gallery_id FROM user_badge ub
JOIN user u ON u.id = ub.user_id
WHERE ub.user_id IN (:ids) AND ub.issued_on BETWEEN IFNULL(:from, (SELECT MIN(issued_on) FROM user_badge) ) AND IFNULL(:to, (SELECT MAX(issued_on) FROM user_badge))
GROUP BY ub.gallery_id
LIMIT 100000

--name: select-user-badges-report
SELECT ub.id, ub.visibility, ub.status, ub.expires_on, ub.issued_on, ub.deleted, g.badge_name, g.issuer_name, g.badge_image
FROM user_badge ub
JOIN gallery g ON g.id = ub.gallery_id
WHERE ub.deleted = 0 AND ub.revoked = 0 AND g.id IN (:ids) AND ub.user_id = :user_id AND (ub.expires_on IS NULL OR ub.expires_on > unix_timestamp())
GROUP BY ub.id, ub.visibility, ub.status, ub.issued_on
ORDER BY ub.issued_on DESC

--name: select-user-ids-space-report
SELECT DISTINCT(us.user_id) FROM user_space us
JOIN user u ON us.user_id = u.id
WHERE us.space_id = :space_id

--name: count-shared-badges
SELECT user_id, COUNT(DISTINCT id) AS count
FROM user_badge
WHERE user_id IN (:ids) AND deleted = 0 AND revoked = 0 AND visibility != 'private' AND (expires_on IS NULL OR expires_on > unix_timestamp())
GROUP BY user_id

--name: count-all-user-badges
SELECT user_id, COUNT(DISTINCT id) AS count
FROM user_badge
WHERE user_id IN (:ids) AND deleted = 0 AND revoked = 0 AND (expires_on IS NULL OR expires_on > unix_timestamp())
GROUP BY user_id
