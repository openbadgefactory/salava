-- name: create-space<!
-- create new space
INSERT INTO spaces
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
