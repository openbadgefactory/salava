INSERT IGNORE INTO social_connections_badge (user_id, badge_content_id, ctime)
SELECT user_id, badge_id, ctime FROM user_badge WHERE deleted= 0 AND status = 'accepted';
