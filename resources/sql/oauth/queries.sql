-- name: select-user-id-by-oauth-user-id-and-service
SELECT user_id FROM oauth_user WHERE oauth_user_id = :oauth_user_id AND service = :service

-- name: insert-oauth-user!
INSERT INTO oauth_user (user_id, oauth_user_id, service, ctime) VALUES (:user_id, :oauth_user_id, :service, UNIX_TIMESTAMP())

-- name: insert-user<!
-- create new user
INSERT INTO user (first_name, last_name, role, language, country, profile_picture, activated, ctime, mtime) VALUES (:first_name, :last_name, 'user', :language, :country, :profile_picture, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: insert-user-email!
-- add new email address to user
INSERT INTO user_email (user_id, email, verification_key, verified, primary_address, ctime, mtime) VALUES (:user_id, :email, NULL, 1, 1, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: select-user-id-by-email
SELECT user_id, verified FROM user_email WHERE email = :email

-- name: select-oauth-user-id
SELECT oauth_user_id FROM oauth_user WHERE user_id = :user_id AND service = :service

-- name: select-user-password
SELECT pass FROM user WHERE id = :id

-- name: delete-oauth-user!
DELETE FROM oauth_user WHERE user_id = :user_id AND service = :service

-- name: delete-oauth-user-all-services!
DELETE FROM oauth_user WHERE user_id = :user_id

--name: update-user-last_login!
UPDATE user SET last_login = UNIX_TIMESTAMP() WHERE id = :id

--name: select-oauth-user-service
SELECT service from oauth_user where user_id = :user_id

--name: insert-user-terms<!
INSERT INTO user_terms (user_id, status, ctime) VALUES (:user_id, :status, UNIX_TIMESTAMP());
