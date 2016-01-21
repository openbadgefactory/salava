-- name: select-user-email-addresses
-- get user's email-addresses
SELECT email FROM user_email
       WHERE user_id = :userid

-- name: select-user-primary-email-addresses
-- get user's badges
SELECT email FROM user_email
       WHERE user_id = :userid AND primary_address = 1

-- name: select-email-address
-- check if email address exists
SELECT user_id FROM user_email WHERE email = :email

-- name: select-user-by-email-address
-- get user data by email address
SELECT id, first_name, last_name, pass, activated FROM user AS u
       JOIN user_email AS ue ON ue.user_id = u.id
       WHERE email = :email

-- name: insert-user<!
-- add new user
INSERT INTO user (first_name, last_name, role, language, country, activation_code, ctime, mtime) VALUES (:first_name, :last_name, 'user', :language, :country, :activation_code, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: insert-user-email!
-- add new unverified email address to user
INSERT INTO user_email (user_id, email, verification_key, primary_address, ctime, mtime) VALUES (:user_id, :email, NULL, :primary_address, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: select-activation-code-and-status
SELECT activation_code, activated FROM user WHERE id = :id

-- name: update-user-password-and-activate!
UPDATE user SET activated = 1, pass = :pass, mtime = UNIX_TIMESTAMP() WHERE id = :id



