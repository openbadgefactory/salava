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
