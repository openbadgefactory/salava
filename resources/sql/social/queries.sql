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
SELECT COUNT(id) AS Count FROM badge_message WHERE badge_content_id = :badge_content_id AND deleted=0
       

