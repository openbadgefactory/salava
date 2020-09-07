--name: insert-social-media-stats!
INSERT INTO system_properties (name, value, ctime) VALUES (:name, :value, UNIX_TIMESTAMP())

--name: latest-social-media-stats
SELECT id, value, ctime FROM system_properties
WHERE name = "social_media_share"
ORDER BY id DESC
LIMIT 1

--name: timestamp-social-media-stats
SELECT id, value, ctime FROM system_properties
WHERE name = "social_media_share" AND ctime >= :time
ORDER BY id DESC
LIMIT 500

--name: select-all-spaces
SELECT id FROM space ORDER BY id ASC

--name: select-space-badges
SELECT ub.id FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE us.space_id = :id AND ub.id IN (:ids)
