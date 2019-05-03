--name: select-user
-- get user by id
SELECT id, first_name, last_name, country, language, profile_visibility, profile_picture, role, about, email_notifications, activated FROM user WHERE id = :id AND deleted = 0

--name: select-user-profile-fields
-- get all user's profile fields
SELECT id, field, value, field_order FROM user_profile WHERE user_id = :user_id

--name: delete-user-profile-fields!
DELETE FROM user_profile WHERE user_id = :user_id

--name: insert-user-profile-field!
INSERT INTO user_profile (user_id, field, value, field_order) VALUES (:user_id, :field, :value, :field_order)

--name: update-user-visibility-picture-about!
UPDATE user SET profile_visibility = :profile_visibility, profile_picture = :profile_picture, about = :about, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: insert-user-profile-properties!
REPLACE INTO user_properties (user_id, name, value) VALUES (:user_id, 'profile', :value)

--name: select-user-profile-properties
SELECT value from user_properties where user_id = :user_id and name = 'profile';
