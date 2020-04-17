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

--name: store-user-last-visited!
REPLACE INTO user_properties (user_id, name, value) VALUES (:user_id, 'last_visited', :value );

--name: select-oauth-user-last-login
SELECT last_login FROM user WHERE id = :id;


--name: select-oauth2-auth-code-user
SELECT u.* FROM user u
INNER JOIN oauth2_token t ON u.id = t.user_id
WHERE t.client_id = :client_id AND t.auth_code = :auth_code AND t.refresh_token IS NULL;

--name: select-oauth2-refresh-token-user
SELECT u.*, t.id AS token_id, t.refresh_token FROM user u
INNER JOIN oauth2_token t ON u.id = t.user_id
WHERE t.client_id = :client_id AND t.user_id = :user_id AND t.refresh_token IS NOT NULL;

-- name: delete-oauth2-token!
DELETE FROM oauth2_token WHERE user_id = :user_id AND client_id = :client_id;

-- name: delete-oauth2-auth-code!
DELETE FROM oauth2_token WHERE user_id = :user_id AND client_id = :client_id AND auth_code IS NOT NULL;

-- name: insert-oauth2-auth-code!
INSERT INTO oauth2_token (user_id, client_id, auth_code, ctime, mtime)
    VALUES (:user_id, :client_id, :auth_code, UNIX_TIMESTAMP(), UNIX_TIMESTAMP());

-- name: update-oauth2-auth-code!
UPDATE oauth2_token SET refresh_token = :rtoken, auth_code = NULL, auth_code_challenge = NULL, mtime = UNIX_TIMESTAMP()
WHERE client_id = :client_id AND auth_code = :auth_code;

-- name: update-oauth2-refresh-token!
UPDATE oauth2_token SET refresh_token = :rtoken, mtime = UNIX_TIMESTAMP()
WHERE client_id = :client_id AND id = :id AND user_id = :user_id AND auth_code IS NULL;
