-- name: create-space<!
-- create new space
INSERT INTO space
  (uuid, name, alias, description, url, logo, banner, status, visibility, valid_until, ctime, mtime)
VALUES
  (:uuid, :name, :alias, :description, :url, :logo, :banner, :status, :visibility, :valid_until, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

--name: update-space-information!
UPDATE space SET name = :name, description = :description, url = :url, logo = :logo, banner = :banner, mtime= UNIX_TIMESTAMP(), last_modified_by= :user_id
WHERE id = :id

-- name: select-email-address
-- check if email address exists
SELECT user_id, verified FROM user_email WHERE email = :email

--name: create-space-member!
INSERT INTO user_space
  (user_id, space_id, role, status, ctime, mtime)
VALUES
  (:user_id, :space_id, :role, :status, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

--name: create-pending-space-admin!
INSERT INTO space_admin_pending (space_id, email, ctime) VALUES (:space_id, :email, UNIX_TIMESTAMP())

--name: select-all-spaces
SELECT * FROM space

--name: select-all-active-spaces
SELECT * FROM space WHERE status = 'active'

--name: select-space-by-id
SELECT * FROM space WHERE id = :id

--name: select-space-by-uuid
SELECT * FROM space WHERE uuid = :uuid

--name: select-space-by-name
SELECT * FROM space WHERE name = :name

--name: select-space-by-alias
SELECT id, alias FROM space WHERE alias = :alias

--name: select-deleted-spaces
SELECT id, name, mtime FROM space WHERE status = 'deleted'

--name:select-default-space
SELECT s.id, us.user_id, s.name FROM user_space us
JOIN space s ON s.id = us.space_id
WHERE us.default_space = 1 AND s.status = 'active' AND us.user_id = :user_id

--name: select-space-admins
SELECT us.user_id AS id, us.space_id, us.default_space, u.first_name, u.last_name, u.profile_picture
FROM user_space us
JOIN user u ON us.user_id = u.id
WHERE us.space_id = :space_id AND us.role = 'admin'

--name: select-pending-space-admins
SELECT * FROM space_admin_pending WHERE space_id = :space_id

--name: delete-space!
DELETE FROM space WHERE id = :id

--name: delete-space-members!
DELETE FROM user_space WHERE space_id = :space_id

--name:delete-space-properties!
DELETE FROM space_properties WHERE space_id = :space_id

--name:delete-space-property!
DELETE FROM space_properties WHERE space_id = :space_id AND name = :name

--name: select-primary-address
SELECT email FROM user_email WHERE user_id = :id AND primary_address = 1

--name: select-space-property
SELECT value FROM space_properties WHERE space_id = :id AND name = :name

--name: insert-space-property!
REPLACE INTO space_properties (space_id, name, value) VALUES (:space_id, :name, :value)

--name: soft-delete-space!
UPDATE space SET status = "deleted", last_modified_by = :user_id, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: count-space-members
SELECT COUNT(DISTINCT us.user_id) AS count FROM user_space us
JOIN user u ON u.id = us.user_id
WHERE us.space_id = :id AND us.status = 'accepted'

--name: downgrade-to-member!
UPDATE user_space SET role = 'member', mtime = UNIX_TIMESTAMP() WHERE space_id = :id AND user_id = :admin

--name: upgrade-member-to-admin!
UPDATE user_space SET role = 'admin', mtime = UNIX_TIMESTAMP() WHERE space_id = :id AND user_id = :admin

--name: select-space-members
SELECT us.user_id AS id, us.space_id, us.default_space, u.first_name, u.last_name, u.profile_picture
FROM user_space us
JOIN user u ON us.user_id = u.id
WHERE us.space_id = :space_id AND us.role = 'member'

--name: select-space-members-all
SELECT us.user_id AS id, us.space_id, us.default_space, us.status, u.first_name, u.last_name, u.profile_picture, us.role, us.mtime
FROM user_space us
JOIN user u ON us.user_id = u.id
WHERE us.space_id = :space_id

--name: update-space-status!
UPDATE space SET status = :status, last_modified_by = :user_id, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: select-user-spaces
SELECT us.space_id, us.user_id, us.role, us.default_space, us.status, us.ctime, s.id, s.name, s.logo
FROM user_space us
JOIN space s ON s.id = us.space_id
WHERE us.user_id = :id AND s.status = "active"  AND (s.valid_until IS NULL OR s.valid_until > UNIX_TIMESTAMP())
GROUP BY us.space_id

--name: remove-user-from-space!
DELETE FROM user_space WHERE space_id = :space_id AND user_id = :user_id

--name: reset-default-space-value!
UPDATE user_space SET default_space = 0, mtime = UNIX_TIMESTAMP() WHERE user_id = :user_id

--name: set-default-space!
UPDATE user_space SET default_space = 1, mtime = UNIX_TIMESTAMP() WHERE user_id = :user_id AND space_id = :space_id

--name: select-user-space-role
SELECT role FROM user_space WHERE space_id = :space_id AND user_id = :user_id

--name: update-space-visibility!
UPDATE space SET visibility = :v, last_modified_by = :user_id, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: select-gallery-spaces
SELECT space.id, space.logo, space.ctime, space.mtime, space.name, space.visibility,
      (SELECT CAST(COUNT(DISTINCT us.user_id) AS UNSIGNED)
       FROM user_space us
       JOIN user u ON us.user_id = u.id
       WHERE us.space_id = space.id AND us.status = 'accepted') AS member_count
FROM space space
WHERE space.status = "active" AND space.visibility != "private" AND (space.valid_until IS NULL OR space.valid_until > UNIX_TIMESTAMP())
GROUP BY space.id
ORDER BY
 CASE WHEN :order='name'  THEN space.name END,
 CASE WHEN :order='member_count' THEN member_count END DESC,
 CASE WHEN :order='mtime' THEN MAX(space.ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-spaces-filtered
SELECT space.id, space.logo, space.ctime, space.mtime, space.name, space.visibility,
      (SELECT CAST(COUNT(DISTINCT us.user_id) AS UNSIGNED)
       FROM user_space us
       JOIN user u ON us.user_id = u.id
       WHERE us.space_id = space.id AND us.status = 'accepted') AS member_count
FROM space space
WHERE space.status = "active" AND space.visibility != "private" AND (space.valid_until IS NULL OR space.valid_until > UNIX_TIMESTAMP()) AND space.id IN (:space_ids)
GROUP BY space.id
ORDER BY
 CASE WHEN :order='name'  THEN space.name END,
 CASE WHEN :order='member_count' THEN member_count END DESC,
 CASE WHEN :order='mtime' THEN MAX(space.ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-gallery-spaces-ids-name
SELECT id FROM space
WHERE status = "active" AND visibility != "private" AND (valid_until IS NULL OR valid_until > UNIX_TIMESTAMP()) AND name LIKE :name
ORDER BY ctime DESC
LIMIT 100000

--name: select-gallery-spaces-count
SELECT COUNT(id) AS total
FROM space
WHERE space.status = "active" AND space.visibility != "private" AND (valid_until IS NULL OR valid_until > UNIX_TIMESTAMP())

--name: check-space-member
SELECT user_id, role, status FROM user_space WHERE space_id = :id AND user_id = :user_id

--name: select-space-visibility
SELECT visibility FROM space WHERE id = :id

--name: update-membership-status!
UPDATE user_space SET status = :status, mtime = UNIX_TIMESTAMP() WHERE user_id = :user_id AND space_id = :id

--name: update-last-modifier!
UPDATE space SET last_modified_by = :admin WHERE id = :id

--name: select-expired-spaces
SELECT name, id, valid_until FROM space WHERE status != "expired" AND valid_until < UNIX_TIMESTAMP()

--name: extend-space-subscription!
UPDATE space SET valid_until = :time, last_modified_by = :admin WHERE id = :id
