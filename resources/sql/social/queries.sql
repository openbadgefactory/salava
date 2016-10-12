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
INSERT INTO social_connections_badge (user_id, badge_content_id, ctime)
                   VALUES (:user_id, :badge_content_id, UNIX_TIMESTAMP())

--name: delete-connect-badge!
DELETE FROM social_connections_badge WHERE user_id = :user_id  AND badge_content_id = :badge_content_id

--name: select-connection-badge
SELECT badge_content_id FROM social_connections_badge WHERE user_id = :user_id AND badge_content_id = :badge_content_id

-- name: select-user-connections-badge
-- get users badge connections
SELECT bc.id, bc.name, bc.image_file, bc.description, ic.name AS issuer_content_name, ic.url AS issuer_content_url, MAX(b.ctime) AS ctime, badge_content_id  FROM badge AS b
       JOIN badge_content AS bc ON b.badge_content_id = bc.id
       JOIN issuer_content AS ic ON b.issuer_content_id = ic.id	
       LEFT JOIN user AS u ON b.user_id = u.id
       WHERE b.badge_content_id IN (SELECT badge_content_id FROM social_connections_badge where user_id = :user_id)
	     AND b.status = 'accepted'
       	     AND b.deleted = 0
	     AND b.revoked = 0
	     AND (b.expires_on IS NULL OR b.expires_on > UNIX_TIMESTAMP())
      GROUP BY bc.id, bc.name, bc.image_file, bc.description, ic.name, ic.url, b.badge_content_id
      ORDER BY ctime DESC

