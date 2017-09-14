ALTER TABLE social_connections_badge CHANGE badge_content_id badge_id VARCHAR(255) DEFAULT NULL;
--;;
ALTER TABLE badge_message CHANGE badge_content_id badge_id VARCHAR(255) DEFAULT NULL;
--;;
ALTER TABLE badge_message_view CHANGE badge_content_id badge_id VARCHAR(255) DEFAULT NULL;
