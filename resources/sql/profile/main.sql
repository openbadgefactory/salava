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

--name: delete-showcase-badges!
DELETE FROM user_profile_badge_showcase_has_badge WHERE block_id = :block_id

--name: delete-showcase-block!
DELETE FROM user_profile_badge_showcase WHERE id = :id

--name: insert-showcase-block<!
INSERT INTO user_profile_badge_showcase (user_id, title, format, block_order) VALUES (:user_id, :title, :format, :block_order)

--name: update-badge-showcase-block!
UPDATE user_profile_badge_showcase SET title = :title, format = :format, block_order = :block_order WHERE id = :id AND user_id = :user_id

--name: insert-showcase-badges!
INSERT INTO user_profile_badge_showcase_has_badge (block_id, badge_id, badge_order) VALUES (:block_id, :badge_id, :badge_order)

--name: select-badge-showcase-blocks
SELECT id, "showcase" AS type, title, format, block_order FROM user_profile_badge_showcase WHERE user_id = :user_id

--name: select-showcase-block-content
-- get badges in badge showcase
SELECT DISTINCT ub.id, bc.name, bc.image_file FROM user_badge AS ub
  JOIN user_profile_badge_showcase_has_badge AS pb ON pb.badge_id = ub.id
  JOIN badge AS badge ON (badge.id = ub.badge_id)
  JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
  JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
  WHERE pb.block_id = :block_id
  ORDER BY pb.badge_order

  --name: select-page
  --get profile page tab
  SELECT id, name FROM page WHERE id = :id AND deleted != 1;
