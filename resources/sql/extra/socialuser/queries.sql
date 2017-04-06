
--name: delete-connections-user!
DELETE FROM social_connections_user WHERE owner_id = :owner_id  AND user_id = :user_id

--name: select-connection-badge
SELECT badge_content_id FROM social_connections_badge WHERE user_id = :user_id AND badge_content_id = :badge_content_id

--name: insert-connections-user<!
--add new user connect
INSERT INTO social_connections_user (owner_id, user_id, status, ctime)
 VALUES (:owner_id, :user_id ,(SELECT IFNULL((SELECT value FROM user_properties AS up
WHERE up.user_id = :user_id AND up.name = 'user_connect_accepting'), 'pending')), UNIX_TIMESTAMP())

-- name: select-user-connections-user
-- get users user connections
SELECT DISTINCT u.first_name, u.last_name, u.profile_picture, scu.user_id  FROM social_connections_user AS scu
JOIN user as u ON scu.user_id = u.id
       WHERE scu.owner_id = :owner_id AND scu.status = 'accepted'
       GROUP BY u.first_name, u.last_name, u.profile_picture, scu.user_id
       ORDER BY u.first_name

-- name: select-user-connections-user-pending
-- get users connections
SELECT DISTINCT u.first_name, u.last_name, u.profile_picture, scu.user_id, scu.owner_id  FROM social_connections_user AS scu
JOIN user as u ON scu.owner_id = u.id
       WHERE scu.user_id = :user_id AND scu.status = 'pending'
       GROUP BY u.first_name, u.last_name, u.profile_picture, scu.user_id, scu.owner_id
       ORDER BY u.first_name
       
-- name: select-connections-user
SELECT DISTINCT scu.owner_id, scu.user_id, scu.status, scu.ctime  FROM social_connections_user AS scu
       WHERE scu.owner_id = :owner_id AND scu.user_id = :user_id

-- name: update-user-connections-accepting!
REPLACE INTO user_properties (user_id, name, value)
 VALUES (:user_id, 'user_connect_accepting', :value)

--name: select-user-connections-accepting
SELECT value AS status FROM user_properties WHERE name='user_connect_accepting' and user_id = :user_id

--name: update-connections-user-pending!
UPDATE social_connections_user SET status = :status
WHERE owner_id = :owner_id and user_id = :user_id
