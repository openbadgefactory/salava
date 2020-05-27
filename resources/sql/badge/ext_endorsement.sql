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
SELECT ubee.id, ubee.user_badge_id, ubee.status, ubee.content, ubee.mtime, u.first_name, u.last_name, u.profile_picture, bc.name, bc.image_file
FROM user_badge_endorsement_ext ubee
JOIN user_badge ub ON ub.id=ubee.user_badge_id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN user u ON u.id = ub.user_id
WHERE ubee.issuer_id = :issuer

--name: select-all-endorsement-requests
SELECT rext.id, rext.user_badge_id, rext.status, rext.content, rext.mtime, u.first_name, u.last_name, u.profile_picture, bc.name, bc.image_file
FROM user_badge_endorsement_request_ext rext
JOIN user_ext uext ON uext.email = rext.issuer_email
JOIN user_badge ub ON ub.id=rext.user_badge_id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN user u ON u.id = ub.user_id
WHERE uext.ext_id = :issuer

--name: delete-external-endorsement!
DELETE FROM user_badge_endorsement_ext WHERE id = :id

--name: select-existing-endorsement
SELECT id FROM user_badge_endorsement_ext ubee
WHERE issuer_id = :issuer AND user_badge_id = :ubid AND status != "declined"

--name: select-existing-endorsement-by-email
SELECT ubee.id FROM user_badge_endorsement_ext ubee
JOIN user_ext u ON u.ext_id = ubee.issuer_id
WHERE u.email = :issuer AND user_badge_id = :ubid AND status != "declined"

--name: delete-user-badge-ext-endorsements!
DELETE FROM user_badge_endorsement_ext WHERE user_badge_id = :id

--name: delete-user-badge-ext-endorsement-requests!
DELETE FROM user_badge_endorsement_request_ext WHERE user_badge_id = :id

--name: delete-all-user-endorsements!
DELETE FROM user_badge_endorsement_ext WHERE issuer_id = :issuer

--name: delete-all-user-requests!
DELETE FROM user_badge_endorsement_request_ext WHERE issuer_email = :issuer

--name: delete-sent-external-request!
DELETE FROM user_badge_endorsement_request_ext WHERE id = :id

--name: insert-ext-endorsement-event<!
INSERT INTO social_event (subject, verb, object, type, ctime, mtime) VALUES (:subject, 'endorse_badge_ext', :object, 'badge', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: insert-event-owner!
INSERT INTO social_event_owners (owner, event_id) VALUES (:object, :event_id)

--name: select-ext-endorsement-receiver-by-badge-id
SELECT ub.user_id AS id FROM user_badge_endorsement_ext AS ubee
INNER JOIN user_badge AS ub ON (ub.id = ubee.user_badge_id)
WHERE ubee.id = :id

--name: select-ext-endorsement-events
SELECT se.subject, se.verb, se.object, se.ctime, se.type, seo.event_id, seo.last_checked, u.image_file AS issuer_image,u.email,
    bc.name, bc.image_file, seo.hidden, ube.id, ube.user_badge_id, ube.issuer_id, ube.issuer_name, ube.issuer_url, ube.content, ube.status, ube.mtime
FROM social_event_owners AS seo
INNER JOIN social_event AS se ON seo.event_id = se.id
INNER JOIN user_badge_endorsement_ext AS ube ON (ube.id = se.object)
INNER JOIN user_badge AS ub ON (ub.id = ube.user_badge_id)
INNER JOIN user_ext AS u ON (ube.issuer_id = u.ext_id)
INNER JOIN badge AS badge ON (badge.id = ub.badge_id)
INNER JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
INNER JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
WHERE seo.owner = :user_id AND se.type = 'badge' AND se.verb = 'endorse_badge_ext' AND ube.status = 'pending'
ORDER BY se.ctime DESC
LIMIT 1000

--name: update-ext-endorsement-status!
UPDATE user_badge_endorsement_ext SET status = :status, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: select-ext-received-endorsements
SELECT ube.id, ube.user_badge_id, ube.issuer_id, ube.issuer_name, ube.issuer_url, ube.content, ube.status, ube.mtime,
endorser.image_file AS issuer_image, endorser.email, bc.name, bc.image_file, bc.description
FROM user_badge_endorsement_ext AS ube
LEFT JOIN user_ext AS endorser ON endorser.ext_id = ube.issuer_id
JOIN user_badge AS ub ON ub.id = ube.user_badge_id
JOIN user AS recepient ON  ub.user_id = recepient.id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE recepient.id = :user_id
ORDER BY ube.mtime DESC

--name: select-sent-ext-endorsement-requests
--select all endorsement requests sent by user
SELECT uber.id, uber.user_badge_id, uber.content, uber.status, issuer.name AS issuer_name, issuer.image_file AS issuer_image, issuer.email, uber.ctime, uber.mtime, bc.name, bc.image_file
FROM user_badge_endorsement_request_ext AS uber
JOIN user_badge AS ub ON ub.id= uber.user_badge_id
JOIN user AS u ON u.id = ub.user_id
JOIN user_ext AS issuer ON uber.issuer_email = issuer.email
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE uber.status = 'pending' AND u.id = :id

--name: pending-user-badge-ext-endorsement-count-multi
--get user badge's pending external endorsement
SELECT COUNT(ube.id) AS count, ube.user_badge_id FROM user_badge_endorsement_ext AS ube WHERE ube.user_badge_id IN (:user_badge_ids) AND ube.status = 'pending'
GROUP BY ube.user_badge_id

--name:pending-ext-endorsement-count
SELECT COUNT(id) AS count FROM user_badge_endorsement_ext WHERE user_badge_id = :id AND status = 'pending';

--name: select-user-badge-ext-endorsements
SELECT ube.id, ube.user_badge_id, ube.issuer_id, u.name AS issuer_name, ube.issuer_url, ube.content, ube.status, ube.mtime,u.image_file AS issuer_image
FROM user_badge_endorsement_ext AS ube
LEFT JOIN user_ext AS u on u.ext_id = ube.issuer_id
WHERE ube.user_badge_id = :user_badge_id
ORDER BY ube.mtime DESC

--name: accepted-ext-endorsement-count
SELECT COUNT(id) AS count FROM user_badge_endorsement_ext WHERE user_badge_id = :id AND status = 'accepted';
