-- name: select-user-email-addresses
-- get user's email-addresses
SELECT email, verified, primary_address, backpack_id, ctime, mtime FROM user_email
       WHERE user_id = :user_id

-- name: select-user-primary-email-addresses
-- get user's badges
SELECT email FROM user_email
       WHERE user_id = :userid AND primary_address = 1

-- name: select-email-address
-- check if email address exists
SELECT user_id FROM user_email WHERE email = :email

-- name: select-user-by-email-address
-- get user data by email address
SELECT id, first_name, last_name, pass, activated, primary_address, verified, verification_key, language, profile_picture, country FROM user AS u
       JOIN user_email AS ue ON ue.user_id = u.id
       WHERE email = :email

--name: select-user
-- get user by id
SELECT id, first_name, last_name, country, language, profile_visibility, profile_picture, about FROM user WHERE id = :id

--name: select-user-profile-fields
-- get all user's profile fields
SELECT id, field, value, field_order FROM user_profile WHERE user_id = :user_id

-- name: insert-user<!
-- add new user
INSERT INTO user (first_name, last_name, role, language, country, ctime, mtime) VALUES (:first_name, :last_name, 'user', :language, :country, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: insert-user-email!
-- add new unverified email address to user
INSERT INTO user_email (user_id, email, verification_key, verified, primary_address, ctime, mtime) VALUES (:user_id, :email, :verification_key, 0, :primary_address, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: select-activation-code-and-status
SELECT verification_key, activated, ue.mtime FROM user AS u
       JOIN user_email AS ue ON ue.user_id = u.id
       WHERE id = :id AND primary_address = 1

-- name: update-user-password-and-activate!
UPDATE user SET activated = 1, pass = :pass, mtime = UNIX_TIMESTAMP() WHERE id = :id

-- name: select-password-by-user-id
-- get user password by user-id
SELECT pass FROM user WHERE id = :id

-- name: update-user!
-- update basic user information
UPDATE user SET first_name = :first_name, last_name = :last_name, country = :country, language = :language, mtime = UNIX_TIMESTAMP() WHERE id = :id

-- name: update-password!
-- change user password
UPDATE user SET pass = :pass WHERE id = :id

-- name: delete-email-address!
-- delete email address
DELETE FROM user_email WHERE email = :email AND user_id = :user_id AND primary_address = 0

--name: update-primary-email-address!
UPDATE user_email SET primary_address = 1 WHERE user_id = :user_id AND email = :email

--name: update-other-addresses!
UPDATE user_email SET primary_address = 0 WHERE user_id = :user_id AND email != :email

--name: update-verify-email-address!
UPDATE user_email SET verified = 1 WHERE user_id = :user_id AND email = :email

--name: update-verify-primary-email-address!
UPDATE user_email SET verified = 1 WHERE user_id = :user_id AND primary_address = 1

--name: update-primary-email-address-verification-key!
UPDATE user_email SET verification_key = :verification_key, mtime = UNIX_TIMESTAMP() WHERE email = :email AND primary_address = 1

--name: update-set-primary-email-address-verification-key-null!
UPDATE user_email SET verification_key = NULL WHERE user_id = :user_id AND primary_address = 1

--name: update-user-visibility!
UPDATE user SET profile_visibility = :profile_visibility, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-user-visibility-picture-about!
UPDATE user SET profile_visibility = :profile_visibility, profile_picture = :profile_picture, about = :about, mtime = UNIX_TIMESTAMP() WHERE id = :id
