--name: select-user-location
SELECT location_lat AS lat, location_lng AS lng FROM user
WHERE id = :user AND location_lat IS NOT NULL AND location_lng IS NOT NULL;

--name: select-user-country
SELECT country FROM user WHERE id = :user;

--name: select-user-badge-location
SELECT COALESCE(ub.location_lat, u.location_lat) AS lat, COALESCE(ub.location_lng, u.location_lng) AS lng FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.id = :badge AND ub.user_id = :user;

--name: update-user-location!
UPDATE user SET location_lat = :lat, location_lng = :lng, mtime = UNIX_TIMESTAMP() WHERE id = :user;

--name: update-user-badge-location!
UPDATE user_badge SET location_lat = :lat, location_lng = :lng, mtime = UNIX_TIMESTAMP() WHERE id = :badge AND user_id = :user;

--name: select-explore-users
SELECT id, location_lat AS lat, location_lng AS lng FROM user
WHERE location_lat IS NOT NULL AND location_lng IS NOT NULL AND deleted = 0;

--name: select-explore-badges
SELECT ub.id, ub.badge_id,
    COALESCE(ub.location_lat, (u.location_lat + (RAND() * 0.003) - (RAND() * 0.003))) AS lat,
    COALESCE(ub.location_lng, (u.location_lng + (RAND() * 0.006) - (RAND() * 0.006))) AS lng
FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.deleted = 0 AND ub.visibility != 'private' AND ub.status = 'accepted'
HAVING lat IS NOT NULL AND lng IS NOT NULL
ORDER BY ub.mtime DESC
LIMIT 1000;
