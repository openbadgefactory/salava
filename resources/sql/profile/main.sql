--name: select-user
-- get user by id
SELECT id, first_name, last_name, country, language, profile_visibility, profile_picture, role, about, email_notifications, activated FROM user WHERE id = :id AND deleted = 0

--name: select-user-profile-fields
-- get all user's profile fields
SELECT id, field, value, field_order FROM user_profile WHERE user_id = :user_id
