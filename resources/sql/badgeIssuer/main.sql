--name: insert-selfie-badge!
INSERT INTO selfie_badge (id, creator_id, name, description, image, issuable_from_gallery, deleted, ctime, mtime)
VALUES (:id, :creator_id, :name, :description, :image, :ifg, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: update-selfie-badge!
UPDATE selfie_badge SET name = :name, description = :description, image = :image, issuable_from_gallery= :ifg, mtime = UNIX_TIMESTAMP()
WHERE id = :id AND creator_id = :creator_id

--name: soft-delete-selfie-badge!
UPDATE selfie_badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id AND creator_id = :creator_id

--name: hard-delete-selfie-badge!
DELETE FROM selfie_badge WHERE id = :id AND creator_id = :creator_id

--name: get-user-selfie-badges
SELECT * FROM selfie_badge WHERE creator_id = :creator_id AND deleted = 0
GROUP BY mtime DESC
