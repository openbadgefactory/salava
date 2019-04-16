--name: select-user-location
SELECT location_lat AS lat, location_lng AS lng FROM user
WHERE id = :user AND location_lat IS NOT NULL AND location_lng IS NOT NULL;

--name: select-user-location-public
SELECT location_lat AS lat, location_lng AS lng FROM user
WHERE id = :user AND location_public = 1 AND profile_visibility = 'public'
    AND location_lat IS NOT NULL AND location_lng IS NOT NULL;

--name: select-user
SELECT * FROM user WHERE id = :user;

--name: select-user-badge-location
SELECT COALESCE(ub.location_lat, u.location_lat) AS lat, COALESCE(ub.location_lng, u.location_lng) AS lng FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE (ub.user_id = :user OR ub.visibility != 'private')
    AND ub.id = :badge AND ub.deleted = 0 AND ub.status != 'declined'
    AND u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL;

--name: select-user-badge-location-public
SELECT COALESCE(ub.location_lat, u.location_lat) AS lat, COALESCE(ub.location_lng, u.location_lng) AS lng FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.id = :badge
    AND u.location_public = 1 AND ub.deleted = 0 AND ub.status = 'accepted' AND ub.visibility = 'public'
    AND u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL;

--name: update-user-location!
UPDATE user SET location_lat = :lat, location_lng = :lng, mtime = UNIX_TIMESTAMP() WHERE id = :user;

--name: update-user-location-public!
UPDATE user SET location_public = :pub, mtime = UNIX_TIMESTAMP() WHERE id = :user;

--name: update-user-badge-location!
UPDATE user_badge SET location_lat = :lat, location_lng = :lng, mtime = UNIX_TIMESTAMP()
WHERE id = :badge AND user_id = :user AND deleted = 0;

--name: reset-user-badge-location!
UPDATE user_badge SET location_lat = NULL, location_lng = NULL, mtime = UNIX_TIMESTAMP()
WHERE user_id = :user AND location_lat IS NOT NULL AND location_lng IS NOT NULL AND deleted = 0;

--name: select-explore-badge
SELECT ub.id, ub.user_id, ub.badge_id,
    ub.location_lat  AS badge_lat, u.location_lat AS user_lat,
    ub.location_lng  AS badge_lng, u.location_lng AS user_lng
FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.badge_id = :badge AND ub.deleted = 0 AND ub.visibility != 'private' AND ub.status = 'accepted' AND ub.deleted = 0
    AND u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL
ORDER BY ub.mtime DESC
LIMIT 250;

--name: select-explore-user-ids-latlng
SELECT id FROM user
WHERE location_lat > :min_lat AND location_lat <= :max_lat
    AND location_lng > :min_lng AND location_lng <= :max_lng;

--name: select-explore-user-ids-public
SELECT id FROM user
WHERE id IN (:user) AND location_public = 1 AND profile_visibility = 'public';

--name: select-explore-user-ids-name
SELECT id FROM user
WHERE id IN (:user) AND CONCAT(first_name, ' ', last_name) LIKE :name;

--name: select-explore-users
SELECT id, first_name, last_name,
    location_lat AS lat, location_lng AS lng
FROM user
WHERE id IN (:user)
ORDER BY mtime DESC
LIMIT 250;

--name: select-explore-badge-ids-latlng
SELECT DISTINCT ub.id FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.deleted = 0 AND ub.visibility != 'private' AND ub.status = 'accepted'
    AND u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL
    AND (
        (u.location_lat > :min_lat AND u.location_lat <= :max_lat AND u.location_lng > :min_lng AND u.location_lng <= :max_lng)
        OR
        (ub.location_lat > :min_lat AND ub.location_lat <= :max_lat AND ub.location_lng > :min_lng AND ub.location_lng <= :max_lng)
    );

--name: select-explore-badge-ids-public
SELECT DISTINCT ub.id FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
WHERE ub.id IN (:badge) AND ub.visibility = 'public' AND u.location_public = 1;

--name: select-explore-badge-ids-tag
SELECT DISTINCT ub.id FROM user_badge ub
INNER JOIN badge_badge_content bc ON ub.badge_id = bc.badge_id
INNER JOIN badge_content_tag t ON bc.badge_content_id = t.badge_content_id
WHERE ub.id IN (:badge) AND t.tag = :tag;

--name: select-explore-badge-ids-name
SELECT DISTINCT ub.id FROM user_badge ub
INNER JOIN badge_badge_content bc ON ub.badge_id = bc.badge_id
INNER JOIN badge_content c ON bc.badge_content_id = c.id
WHERE ub.id IN (:badge) AND c.name LIKE :name;

--name: select-explore-badge-ids-issuer
SELECT DISTINCT ub.id FROM user_badge ub
INNER JOIN badge_issuer_content bc ON ub.badge_id = bc.badge_id
INNER JOIN issuer_content c ON bc.issuer_content_id = c.id
WHERE ub.id IN (:badge) AND c.name LIKE :issuer;

--name: select-explore-badges
SELECT ub.id, ub.user_id, ub.badge_id,
    c.name AS badge_name, c.image_file AS badge_image, i.name AS issuer_name,
    ub.location_lat  AS badge_lat, u.location_lat AS user_lat,
    ub.location_lng  AS badge_lng, u.location_lng AS user_lng
FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
INNER JOIN badge b ON ub.badge_id = b.id
INNER JOIN badge_badge_content bc ON b.id = bc.badge_id
INNER JOIN badge_content c ON (bc.badge_content_id = c.id AND c.language_code = b.default_language_code)
INNER JOIN badge_issuer_content ic ON b.id = ic.badge_id
INNER JOIN issuer_content i ON (ic.issuer_content_id = i.id AND i.language_code = b.default_language_code)
WHERE ub.id IN (:badge) AND u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL
ORDER BY ub.mtime DESC
LIMIT 250;


--name: select-explore-taglist
SELECT DISTINCT t.tag FROM badge_content_tag t
INNER JOIN badge_badge_content bc ON bc.badge_content_id = t.badge_content_id
INNER JOIN user_badge ub ON bc.badge_id = ub.badge_id
INNER JOIN user u ON ub.user_id = u.id
WHERE u.location_public >= :min_pub AND ub.deleted = 0 AND ub.visibility IN (:visibility) AND ub.status = 'accepted'
    AND ((u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL) OR (ub.location_lat IS NOT NULL AND ub.location_lng IS NOT NULL))
ORDER BY ub.mtime
LIMIT 1000;

--name: select-explore-badgelist
SELECT DISTINCT c.name FROM badge_content c
INNER JOIN badge_badge_content bc ON bc.badge_content_id = c.id
INNER JOIN user_badge ub ON bc.badge_id = ub.badge_id
INNER JOIN user u ON ub.user_id = u.id
WHERE u.location_public >= :min_pub AND ub.deleted = 0 AND ub.visibility IN (:visibility) AND ub.status = 'accepted'
    AND ((u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL) OR (ub.location_lat IS NOT NULL AND ub.location_lng IS NOT NULL))
ORDER BY ub.mtime
LIMIT 1000;

--name: select-explore-issuerlist
SELECT DISTINCT c.name FROM issuer_content c
INNER JOIN badge_issuer_content bc ON bc.issuer_content_id = c.id
INNER JOIN user_badge ub ON bc.badge_id = ub.badge_id
INNER JOIN user u ON ub.user_id = u.id
WHERE u.location_public >= :min_pub AND ub.deleted = 0 AND ub.visibility IN (:visibility) AND ub.status = 'accepted'
    AND ((u.location_lat IS NOT NULL AND u.location_lng IS NOT NULL) OR (ub.location_lat IS NOT NULL AND ub.location_lng IS NOT NULL))
ORDER BY ub.mtime
LIMIT 1000;

