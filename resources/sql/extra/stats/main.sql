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
