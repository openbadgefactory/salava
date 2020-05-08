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
REPLACE INTO user_ext (id, ext_id, url, name, description, image_file, email, ctime, mtime)
VALUES (:id, :ext_id, :url, :name, :description, :image_file, :email, :ctime, UNIX_TIMESTAMP())

--name: get-external-endorser
SELECT * FROM user_ext WHERE ext_id = :id

--name: insert-external-endorsement<!
INSERT INTO user_badge_endorsement_ext (external_id,user_badge_id, issuer_id, issuer_name, issuer_url, content, status, ctime, mtime)
VALUES (:external_id, :user_badge_id, :issuer_id, :issuer_name, :issuer_url, :content, 'pending', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: update-external-endorsement!
UPDATE user_badge_endorsement_ext SET issuer_name = :issuer_name, issuer_url = :issuer_url, content = :content, status = 'pending', mtime = UNIX_TIMESTAMP()
WHERE id = :id AND user_badge_id = :ubid AND issuer_id = :isid

--name: select-external-badge-request-by-issuerid
SELECT rext.id, rext.status FROM user_badge_endorsement_request_ext rext
JOIN user_ext uext ON uext.email = rext.issuer_email
WHERE rext.user_badge_id = :ubid AND uext.ext_id = :isid

--name: update-request-status!
UPDATE user_badge_endorsement_request_ext SET status = :status, mtime = UNIX_TIMESTAMP()
WHERE user_badge_id = :ubid AND issuer_email = :e

--name: select-user-badge-issuer-endorsement
SELECT ubee.id, ubee.user_badge_id, ubee.status, ubee.content, ubee.mtime, u.first_name, u.last_name, u.profile_picture
FROM user_badge_endorsement_ext ubee
JOIN user_badge ub ON ub.id=ubee.user_badge_id
JOIN user u ON u.id = ub.user_id
WHERE ubee.issuer_id = :issuer AND ubee.user_badge_id = :ubid

--name: select-all-issuer-endorsements
SELECT ubee.id, ubee.user_badge_id, ubee.status, ubee.content, ubee.mtime, u.first_name, u.last_name, u.profile_picture
FROM user_badge_endorsement_ext ubee
JOIN user_badge ub ON ub.id=ubee.user_badge_id
JOIN user u ON u.id = ub.user_id
WHERE ubee.issuer_id = :issuer

--name: delete-external-endorsement!
DELETE FROM user_badge_endorsement_ext WHERE id = :id
