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
SELECT user_id, verified FROM user_email WHERE email = :email

-- name: select-user-by-email-address
-- get user data by email address
SELECT id, first_name, last_name, pass, activated, primary_address, verified, verification_key, language, role, profile_picture, country, deleted FROM user AS u
       JOIN user_email AS ue ON ue.user_id = u.id
       WHERE email = :email

-- name: select-user-by-id
-- get user data by user id
SELECT id, first_name, last_name, pass, activated FROM user WHERE id = :id AND deleted = 0

--name: select-user
-- get user by id
SELECT id, first_name, last_name, country, language, profile_visibility, profile_picture, role, about, email_notifications, activated FROM user WHERE id = :id AND deleted = 0

--name: select-user-with-register-last-login
SELECT id, first_name, last_name, country, language, profile_visibility, profile_picture, role, about, last_login, ctime, deleted, activated, IF(pass IS NULL, 0,1) AS has_password FROM user WHERE id = :id

--name: select-user-password
SELECT pass FROM user WHERE id = :id

--name: select-user-profile-fields
-- get all user's profile fields
SELECT id, field, value, field_order FROM user_profile WHERE user_id = :user_id

-- name: insert-user<!
-- add new user
INSERT INTO user (first_name, last_name, role, language, country, ctime, mtime, email_notifications, pass) VALUES (:first_name, :last_name, 'user', :language, :country, UNIX_TIMESTAMP(), UNIX_TIMESTAMP(), :email_notifications, :pass)

-- name: insert-user-email!
-- add new unverified email address to user
INSERT INTO user_email (user_id, email, verification_key, verified, primary_address, ctime, mtime) VALUES (:user_id, :email, :verification_key, 0, :primary_address, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: select-activation-code-and-status
SELECT verification_key, activated, ue.mtime, email, primary_address FROM user AS u
       JOIN user_email AS ue ON ue.user_id = u.id
       WHERE id = :id AND (primary_address = 1 OR verified = 1) ORDER BY ue.mtime DESC LIMIT 1

-- name: update-user-password-and-activate!
UPDATE user SET activated = 1, pass = :pass, mtime = UNIX_TIMESTAMP() WHERE id = :id

-- name: update-user-activate!
UPDATE user SET activated = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id

-- name: select-password-by-user-id
-- get user password by user-id
SELECT pass FROM user WHERE id = :id

-- name: update-user!
-- update basic user information
UPDATE user SET first_name = :first_name, last_name = :last_name, country = :country, language = :language, mtime = UNIX_TIMESTAMP(), email_notifications = :email_notifications WHERE id = :id

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
UPDATE user_email SET verified = 1, verification_key = NULL WHERE user_id = :user_id AND email = :email

--name: update-verify-primary-email-address!
UPDATE user_email SET verified = 1 WHERE user_id = :user_id AND primary_address = 1

--name: update-primary-email-address-verification-key!
UPDATE user_email SET verification_key = :verification_key, mtime = UNIX_TIMESTAMP() WHERE email = :email AND primary_address = 1

--name: update-email-address-verification-key!
UPDATE user_email SET verification_key = :verification_key, mtime = UNIX_TIMESTAMP() WHERE email = :email

--name: update-verified-email-address-verification-key!
UPDATE user_email SET verification_key = :verification_key, mtime = UNIX_TIMESTAMP() WHERE email = :email

--name: update-set-primary-email-address-verification-key-null!
UPDATE user_email SET verification_key = NULL WHERE user_id = :user_id AND primary_address = 1

--name: select-email-by-verification-key
SELECT user_id, email, verified, verification_key, mtime FROM user_email WHERE verification_key = :verification_key AND user_id = :user_id

--name: update-user-visibility!
UPDATE user SET profile_visibility = :profile_visibility, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-user-visibility-picture-about!
UPDATE user SET profile_visibility = :profile_visibility, profile_picture = :profile_picture, about = :about, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: update-email-backpack-id!
UPDATE user_email SET backpack_id = :backpack_id WHERE user_id = :user_id AND email = :email

--name: delete-user-profile-fields!
DELETE FROM user_profile WHERE user_id = :user_id

--name: insert-user-profile-field!
INSERT INTO user_profile (user_id, field, value, field_order) VALUES (:user_id, :field, :value, :field_order)

--name: select-user-files-id-path
SELECT id, path FROM user_file WHERE user_id = :user_id

--name: select-user-badge-ids
SELECT id  FROM user_badge WHERE user_id = :user_id

--name: update-user-badges-set-deleted!
UPDATE badge SET visibility = 'private', deleted = 1 WHERE user_id = :user_id

--name: delete-user-badge-views!
DELETE FROM badge_view WHERE user_id = :user_id

--name: update-user-badge-messages-set-removed!
UPDATE badge_message SET message = 'removed by owner', mtime = UNIX_TIMESTAMP() WHERE user_id= :user_id

--name: delete-user-badge-message-views!
DELETE FROM badge_message_view WHERE user_id= :user_id

--name: delete-user-badge!
DELETE FROM user_badge WHERE user_id= :user_id

--name: delete-user-pending-badges!
DELETE pfb FROM pending_factory_badge AS pfb
LEFT JOIN user_email AS ue ON pfb.email=ue.email
WHERE ue.user_id = user_id

--name: delete-user-badge-congratulations!
DELETE FROM badge_congratulation WHERE user_id = :user_id

--name: update-user-pages-set-deleted!
UPDATE page SET visibility = 'private' WHERE user_id = :user_id

--name: delete-user-profile!
DELETE FROM user_profile WHERE user_id = :user_id

--name: delete-email-addresses!
DELETE FROM user_email WHERE user_id = :user_id

--name: delete-user!
DELETE FROM user WHERE id = :id

--name: update-user-set-deleted!
UPDATE user SET first_name = :first_name, last_name = :last_name, mtime = UNIX_TIMESTAMP(), email_notifications = 0, deleted = 1, role = 'deleted', profile_picture = NULL, profile_visibility = 'internal', about = NULL  WHERE id = :id

--name: update-user-email-set-deleted!
UPDATE user_email SET email = :deletedemail, backpack_id = NULL WHERE user_id = :user_id AND email = :email

--name: update-user-last_login!
UPDATE user SET last_login = UNIX_TIMESTAMP() WHERE id = :id



--name: select-user-and-primary-address
SELECT u.first_name, u.last_name, ue.email, language, role FROM user AS u
       JOIN user_email AS ue ON ue.user_id = u.id
       WHERE u.id = :id and ue.primary_address = 1;

--name: select-userid-from-event-owners
select distinct u.id, u.first_name, u.last_name, ue.email, u.language, u.role from social_event_owners AS seo
       JOIN user_email AS ue ON ue.user_id = seo.owner
       JOIN user AS u ON u.id = seo.owner
       WHERE u.email_notifications = 1 AND ue.primary_address = 1 AND u.deleted= 0 AND u.activated = 1;

--name: delete-all-user-events!
DELETE FROM social_event WHERE subject = :subject

--name: delete-user-social-events!
DELETE FROM social_event_owners WHERE owner = :owner;

--name: delete-social-connections-user-following!
DELETE FROM social_connections_user WHERE owner_id = :owner_id

--name: delete-social-connections-badge!
DELETE FROM social_connections_badge WHERE user_id = :user_id

