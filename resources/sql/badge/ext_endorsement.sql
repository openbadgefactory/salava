--name: select-external-request-by-email
SELECT id, status FROM user_badge_endorsement_request_ext WHERE user_badge_id = :user_badge_id AND issuer_email = :email AND status = 'pending'

--name: request-endorsement-ext!
-- Request external endorsements
INSERT INTO user_badge_endorsement_request_ext (user_badge_id, status, content, issuer_email, ctime, mtime)
VALUES (:id, 'pending', :content, :email, UNIX_TIMESTAMP(), UNIX_TIMESTAMP()) 
