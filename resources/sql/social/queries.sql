--name: insert-badge-message<!
--add new badge-message
INSERT INTO badge_message (badge_content_id, user_id, message, ctime, mtime)
                   VALUES (:badge_content_id, :user_id, :message, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: select-badge-messages
--get badge's messages
SELECT bm.id, bm.badge_content_id, bm.message, bm.ctime, bm.user_id, u.first_name, u.last_name, u.profile_picture FROM badge_message bm
       JOIN user AS u ON (u.id = bm.user_id)
       WHERE badge_content_id = :badge_content_id AND bm.deleted=0
       ORDER BY bm.ctime DESC

--name: select-badge-messages-limit
--get badge's messages
SELECT bm.id, bm.badge_content_id, bm.message, bm.ctime, bm.user_id, u.first_name, u.last_name, u.profile_picture FROM badge_message bm
       JOIN user AS u ON (u.id = bm.user_id)
       WHERE badge_content_id = :badge_content_id AND bm.deleted=0
       ORDER BY bm.ctime DESC
       LIMIT :limit OFFSET :offset


--name: select-badge-messages-count
--get badge's messages
SELECT ctime, user_id FROM badge_message WHERE badge_content_id = :badge_content_id AND deleted=0
       
--name: update-badge-message-deleted!
UPDATE badge_message SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :message_id

--name: select-badge-message-owner
SELECT user_id FROM badge_message where id = :message_id

--name: replace-badge-message-view!
REPLACE INTO badge_message_view (user_id, badge_content_id, mtime)
       VALUES (:user_id, :badge_content_id, UNIX_TIMESTAMP())

--name: select-badge-message-last-view
SELECT mtime FROM badge_message_view where badge_content_id = :badge_content_id AND user_id = :user_id


--name: insert-connect-badge<!
--add new connect with badge
INSERT IGNORE INTO social_connections_badge (user_id, badge_content_id, ctime)
                   VALUES (:user_id, :badge_content_id, UNIX_TIMESTAMP())

--name: delete-connect-badge!
DELETE FROM social_connections_badge WHERE user_id = :user_id  AND badge_content_id = :badge_content_id

--name: delete-connect-badge-by-badge-id!
DELETE FROM social_connections_badge WHERE user_id = :user_id  AND badge_content_id =  (SELECT badge_content_id from badge where id = :badge_id AND user_id = :user_id) 

--name: select-connection-badge
SELECT badge_content_id FROM social_connections_badge WHERE user_id = :user_id AND badge_content_id = :badge_content_id

-- name: select-user-connections-badge
-- get users badge connections
SELECT DISTINCT bc.id, bc.name, bc.image_file, bc.description FROM social_connections_badge AS scb
       JOIN badge_content AS bc ON scb.badge_content_id = bc.id
       WHERE scb.user_id = :user_id
      GROUP BY bc.id, bc.name, bc.image_file, bc.description
      ORDER BY bc.name ASC



--name: insert-social-event<!
INSERT INTO social_event (subject, verb, object, type, ctime, mtime) VALUES (:subject, :verb, :object, :type, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: insert-event-owners!
INSERT INTO social_event_owners (owner, event_id) VALUES (:owner, :event_id) 


--name: select-users-from-connections-badge
SELECT user_id AS owner from social_connections_badge where badge_content_id = :badge_content_id


--name: select-user-events
SELECT se.subject, se.verb, se.object, se.ctime, seo.event_id, bc.name, bc.image_file, seo.hidden FROM social_event_owners AS seo
     JOIN social_event AS se ON seo.event_id = se.id
     JOIN badge_content AS bc ON se.object = bc.id
     JOIN social_connections_badge AS scb ON :user_id = scb.user_id
     WHERE owner = :user_id AND se.type = 'badge' AND se.object = scb.badge_content_id
     ORDER BY se.ctime DESC
     LIMIT 1000




--name: select-user-new-messages
SELECT bmv.badge_content_id, bm.user_id, bm.message, bm.ctime, u.first_name, u.last_name, u.profile_picture, bmv.mtime AS last_viewed from badge_message_view AS bmv
       JOIN badge_message AS bm ON bmv.badge_content_id = bm.badge_content_id AND bm.deleted = 0
       JOIN user AS u ON (u.id = bm.user_id)
       where bmv.user_id = :user_id
       ORDER BY bm.ctime ASC


--name: select-messages-with-badge-content-id
SELECT bmv.badge_content_id, bm.user_id, bm.message, bm.ctime, u.first_name, u.last_name, u.profile_picture, bmv.mtime AS last_viewed from badge_message as bm
JOIN user AS u ON (u.id = bm.user_id)
JOIN badge_message_view AS bmv ON bm.badge_content_id = bmv.badge_content_id AND :user_id =  bmv.user_id
WHERE bm.badge_content_id IN (:badge_content_ids) AND bm.deleted = 0
ORDER BY bm.ctime DESC
LIMIT 100


--name: update-hide-user-event!
UPDATE social_event_owners SET hidden = 1 WHERE event_id = :event_id AND owner = :user_id


--name: select-badge-content-id-by-message-id
SELECT badge_content_id from badge_message where id = :message_id

--name: select-badge-content-id-by-badge-id
SELECT badge_content_id from badge where id = :badge_id


