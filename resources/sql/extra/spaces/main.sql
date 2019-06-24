-- name: create-space<!
-- create new space
INSERT INTO space
  (uuid, name, description, logo, banner, status, visibility, ctime, mtime, last_modified_by)
VALUES
  (:uuid, :name, :description, :logo, :banner, 'active', 'closed', UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

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

--name: select-space-by-id
SELECT * FROM space WHERE id = :id

--name: select-space-by-uuid
SELECT * FROM space WHERE uuid = :uuid

--name: select-space-by-name
SELECT * FROM space WHERE name = :name

--name: select-space-admins
SELECT * FROM user_space WHERE space_id = :space_id AND role = 'admin'

--name: select-pending-space-admins
SELECT * FROM space_admin_pending WHERE space_id = :space_id
