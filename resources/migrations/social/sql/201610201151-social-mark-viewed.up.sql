INSERT IGNORE INTO badge_message_view (user_id, badge_content_id, mtime)
SELECT user_id, badge_content_id, ctime FROM social_connections_badge
