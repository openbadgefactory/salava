--name: select-user-ids-badge
SELECT DISTINCT ub.user_id, (SELECT COUNT(DISTINCT gallery_id) FROM user_badge WHERE gallery_id IN (:badge_ids) AND user_id = ub.user_id) AS count FROM user_badge ub
WHERE ub.gallery_id IN (:badge_ids) AND ub.issued_on BETWEEN IFNULL(:from, (SELECT MIN(issued_on) FROM user_badge) ) AND IFNULL(:to, (SELECT MAX(issued_on) FROM user_badge))
      AND ub.deleted = 0 AND ub.revoked = 0
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
SELECT ub.id, ub.visibility, ub.status, ub.expires_on, ub.issued_on, ub.deleted, g.badge_name, g.issuer_name, g.badge_image, ub.user_id
FROM user_badge ub
JOIN gallery g ON g.id = ub.gallery_id
WHERE ub.deleted = 0 AND ub.revoked = 0 AND g.id IN (:ids)
GROUP BY ub.user_id, ub.id, ub.visibility, ub.status, ub.issued_on
ORDER BY ub.issued_on DESC

--name: select-users-for-report
SELECT u.id, u.profile_picture, u.profile_visibility, u.ctime, CONCAT(u.first_name, ' ', u.last_name) AS name, u.activated,  GROUP_CONCAT(DISTINCT ue.email) AS emailaddresses,
CAST(COUNT(DISTINCT ub.id) AS UNSIGNED) AS badgecount,
(SELECT COUNT(DISTINCT id) FROM user_badge WHERE user_id = u.id AND deleted = 0 AND revoked = 0 AND visibility != 'private' AND (expires_on IS NULL OR expires_on > unix_timestamp())) AS sharedbadges
FROM user u
LEFT JOIN user_badge ub ON ub.user_id = u.id
LEFT JOIN user_email ue ON ue.user_id = u.id
WHERE u.id IN (:ids)
GROUP BY u.id

--name: all-gallery-badges
SELECT id FROM gallery

--name: select-all-users-for-report
SELECT id FROM user WHERE deleted = 0
LIMIT 150000
