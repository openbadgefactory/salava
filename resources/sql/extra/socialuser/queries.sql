
--name: delete-connections-user!
DELETE FROM social_connections_user WHERE owner_id = :owner_id  AND user_id = :user_id

--name: select-connection-badge
SELECT badge_content_id FROM social_connections_badge WHERE user_id = :user_id AND badge_content_id = :badge_content_id

--name: insert-connections-user<!
--add new user connect
INSERT INTO social_connections_user (owner_id, user_id, status, ctime)
 VALUES (:owner_id, :user_id, :status, UNIX_TIMESTAMP())

-- name: select-user-connections-user
-- get users user connections
SELECT DISTINCT u.first_name, u.last_name, u.profile_picture, scu.user_id,  scu.status  FROM social_connections_user AS scu
JOIN user as u ON scu.user_id = u.id
       WHERE scu.owner_id = :owner_id 
       GROUP BY u.first_name, u.last_name, u.profile_picture, scu.user_id
       ORDER BY u.first_name


-- name: select-user-followers-connections-user
-- get users user connections
SELECT DISTINCT u.first_name, u.last_name, u.profile_picture, scu.owner_id,  scu.status  FROM social_connections_user AS scu
JOIN user as u ON scu.owner_id = u.id
       WHERE scu.user_id = :user_id 
       GROUP BY u.first_name, u.last_name, u.profile_picture, scu.user_id, scu.owner_id, scu.status
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

--name: select-users-from-connections-user
SELECT  owner_id AS owner from social_connections_user where user_id = :user_id AND status = 'accepted'


--name: select-user-badge-events
-- EVENTS
SELECT se.subject, se.verb, se.object, se.ctime, seo.event_id, seo.last_checked, seo.hidden, se.type, bc.name, bc.image_file, u.first_name, u.last_name, u.profile_picture FROM social_event_owners AS seo
     JOIN social_event AS se ON seo.event_id = se.id
     JOIN social_connections_user AS scu ON :owner_id = scu.owner_id
     JOIN user AS u ON se.subject = u.id
     JOIN badge as b ON se.object = b.id
     JOIN badge_content AS bc ON b.badge_content_id = bc.id
     WHERE seo.owner = :owner_id AND se.subject = scu.user_id AND se.type = 'badge'AND se.verb = 'publish' AND b.visibility <> 'private'
     ORDER BY se.ctime DESC
     LIMIT 1000



--name: select-user-events
-- EVENTS
SELECT seo.owner, se.subject, se.verb, se.object, se.ctime, seo.event_id, seo.last_checked, seo.hidden, se.type, s.id AS s_id, s.first_name AS s_first_name, s.last_name AS s_last_name, s.profile_picture AS s_profile_picture, o.id AS o_id, o.first_name AS o_first_name, o.last_name AS o_last_name, o.profile_picture AS o_profile_picture FROM social_event_owners AS seo
     JOIN social_event AS se ON seo.event_id = se.id
     JOIN social_connections_user AS scu ON :owner_id = scu.owner_id
     INNER JOIN user s ON se.subject = s.id
     INNER JOIN user o ON se.object = o.id
     WHERE seo.owner = :owner_id  AND se.type = 'user' AND se.verb = 'follow' AND (scu.owner_id = se.subject OR scu.owner_id = se.object)
     ORDER BY se.ctime DESC
     LIMIT 1000

 
