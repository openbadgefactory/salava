--name: select-external-request-by-email
SELECT id, status FROM user_badge_endorsement_request_ext WHERE user_badge_id = :user_badge_id AND issuer_email = :email AND status = 'pending'

--name: request-endorsement-ext!
-- Request external endorsements
INSERT INTO user_badge_endorsement_request_ext (user_badge_id, status, content, issuer_email, ctime, mtime)
VALUES (:id, 'pending', :content, :email, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: sent-pending-ext-requests-by-badge-id
SELECT ext.id, ext.user_badge_id, ext.status, ext.content, ext.mtime, ext.issuer_email, uext.id AS issuer_id, uext.url, uext.name, uext.description, uext.image_file
FROM user_badge_endorsement_request_ext AS ext
LEFT JOIN user_ext AS uext ON uext.email = ext.issuer_email
WHERE ext.user_badge_id = :id AND ext.status = 'pending'
ORDER BY ext.mtime DESC

--name: select-badge-owner-emails
SELECT email FROM user_email WHERE user_id = :id

--name: insert-external-user!
INSERT INTO user_ext (ext_id, email, ctime, mtime)
VALUES (:ext_id, :email, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: update-external-user!
REPLACE INTO user_ext (ext_id, url, name, description, image_file,  mtime)
VALUES (:ext_id, :url, :name, :description, :image_file, UNIX_TIMESTAMP())

--name: get-external-endorser
SELECT * FROM user_ext WHERE ext_id = :id
