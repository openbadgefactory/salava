-- name: create-space<!
-- create new space
INSERT INTO space
  (uuid, name, alias, description, logo, banner, status, visibility, valid_until, ctime, mtime)
VALUES
  (:uuid, :name, :alias, :description, :logo, :banner, :status, :visibility, :valid_until, UNIX_TIMESTAMP(),UNIX_TIMESTAMP())

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
SELECT * FROM user_space WHERE space_id = :space_id AND role = 'admin'

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
