--name: insert-selfie-badge<!
REPLACE INTO selfie_badge (id, creator_id, name, description, criteria, image, tags, issuable_from_gallery, deleted, ctime, mtime)
VALUES (:id, :creator_id, :name, :description, :criteria, :image, :tags, :issuable_from_gallery, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: update-selfie-badge!
UPDATE selfie_badge SET name = :name, description = :description, criteria = :criteria, image = :image, tags = :tags, issuable_from_gallery= :issuable_from_gallery, mtime = UNIX_TIMESTAMP()
WHERE id = :id AND creator_id = :creator_id

--name: soft-delete-selfie-badge!
UPDATE selfie_badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id AND creator_id = :creator_id

--name: hard-delete-selfie-badge!
DELETE FROM selfie_badge WHERE id = :id AND creator_id = :creator_id

--name: get-user-selfie-badges
SELECT * FROM selfie_badge WHERE creator_id = :creator_id AND deleted = 0
GROUP BY mtime DESC

--name: get-selfie-badge
SELECT * FROM selfie_badge WHERE id = :id

--name: get-selfie-badge-creator
SELECT creator_id FROM selfie_badge WHERE id = :id

--name: update-user-badge-assertions!
UPDATE user_badge SET assertion_url = :assertion_url, assertion_json = :assertion_json
WHERE id = :id

--name: get-assertion-json
SELECT assertion_json FROM user_badge WHERE id = :id AND deleted = 0
