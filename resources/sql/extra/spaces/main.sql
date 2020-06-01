-- name: create-space<!
-- create new space
INSERT INTO space
  (uuid, name, alias, description, logo, banner, status, visibility, valid_until, ctime, mtime)
VALUES
  (:uuid, :name, :alias, :description, :logo, :banner, :status, :visibility, :valid_until, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

--name: update-space-information!
UPDATE space SET name = :name, description = :description, logo = :logo, banner = :banner, mtime= UNIX_TIMESTAMP(), last_modified_by= :user_id
WHERE id = :id

-- name: select-email-address
-- check if email address exists
SELECT user_id, verified FROM user_email WHERE email = :email

--name: create-space-member!
INSERT INTO user_space
  (user_id, space_id, role, ctime, mtime)
VALUES
  (:user_id, :space_id, :role, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

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

--name: select-primary-address
SELECT email FROM user_email WHERE user_id = :id AND primary_address = 1

--name: select-space-property
SELECT value FROM space_properties WHERE space_id = :id AND name = :name

--name: insert-space-property!
REPLACE INTO space_properties (space_id, name, value) VALUES (:space_id, :name, :value)
