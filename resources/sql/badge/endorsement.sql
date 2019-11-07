--name: request-endorsement<!
--send endorsement request
INSERT INTO user_badge_endorsement_request (user_badge_id, status, content, issuer_name, issuer_id, issuer_url, ctime,mtime)
VALUES (:id, "pending", :content, :issuer_name, :issuer_id, :issuer_url, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: insert-endorsement-request-event<!
INSERT INTO social_event (subject, verb, object, type, ctime, mtime) VALUES (:subject, 'request_endorsement', :object, 'badge', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: insert-event-owner!
INSERT INTO social_event_owners (owner, event_id) VALUES (:object, :event_id)

--name: insert-endorsement-event<!
INSERT INTO social_event (subject, verb, object, type, ctime, mtime) VALUES (:subject, 'endorse_badge', :object, 'badge', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: select-endorsement-events
SELECT se.subject, se.verb, se.object, se.ctime, se.type, seo.event_id, seo.last_checked, u.first_name, u.last_name, u.profile_picture, u.profile_visibility,
    bc.name, bc.image_file, seo.hidden, ube.id, ube.user_badge_id, ube.issuer_id, ube.issuer_name, ube.issuer_url, ube.content, ube.status, ube.mtime
FROM social_event_owners AS seo
INNER JOIN social_event AS se ON seo.event_id = se.id
INNER JOIN user_badge_endorsement AS ube ON (ube.id = se.object)
INNER JOIN user_badge AS ub ON (ub.id = ube.user_badge_id)
INNER JOIN user AS u ON (ube.issuer_id = u.id)
INNER JOIN badge AS badge ON (badge.id = ub.badge_id)
INNER JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
INNER JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
WHERE seo.owner = :user_id AND se.type = 'badge' AND se.verb = 'endorse_badge' AND ube.status = 'pending'
ORDER BY se.ctime DESC
LIMIT 1000

--name: select-endorsement-request-events
SELECT se.subject, se.verb, se.object, se.ctime, se.type, seo.event_id, seo.last_checked,requester.id AS requester_id, requester.first_name, requester.last_name, requester.profile_picture, requester.profile_visibility,
    bc.name, bc.image_file, seo.hidden, uber.id, uber.user_badge_id,uber.content, uber.status, uber.mtime, bc.description, ic.name AS issuer_name, ic.id AS issuer_content_id, ub.issued_on
FROM social_event_owners AS seo
INNER JOIN social_event AS se ON seo.event_id = se.id
INNER JOIN user_badge_endorsement_request AS uber ON (uber.id = se.object)
INNER JOIN user_badge AS ub ON (ub.id = uber.user_badge_id)
INNER JOIN user AS requester ON (ub.user_id = requester.id)
INNER JOIN badge AS badge ON (badge.id = ub.badge_id)
INNER JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
INNER JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
INNER JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
INNER JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE seo.owner = :user_id AND se.type = 'badge' AND se.verb = 'request_endorsement' AND uber.status = 'pending'
ORDER BY se.ctime DESC
LIMIT 1000

--name: select-endorsement-receiver-by-badge-id
SELECT ub.user_id AS id FROM user_badge_endorsement AS ube
INNER JOIN user_badge AS ub ON (ub.id = ube.user_badge_id)
WHERE ube.id = :id

--name: select-endorsement-request-owner-by-badge-id
SELECT uber.issuer_id AS id FROM user_badge_endorsement_request AS uber
WHERE uber.id = :id

--name: select-endorsement-requests
--select all user's endorsement requests
SELECT uber.id, uber.user_badge_id,uber.content, uber.status, uber.mtime,
requester.id AS requester_id, requester.profile_picture, requester.first_name, requester.last_name, bc.name, bc.image_file, bc.description, ic.name AS issuer_name, ic.id AS issuer_content_id, ub.issued_on
FROM user_badge_endorsement_request AS uber
JOIN user_badge AS ub ON ub.id = uber.user_badge_id
LEFT JOIN user AS requester ON requester.id = ub.user_id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id) AND ic.language_code = badge.default_language_code
WHERE uber.issuer_id = :user_id
ORDER BY uber.mtime DESC

--name: select-sent-endorsement-requests
--select all endorsement requests sent by user
SELECT uber.id, uber.user_badge_id, uber.content, uber.status, uber.issuer_id AS requestee_id, uber.issuer_name, issuer.profile_picture, uber.ctime, uber.mtime, bc.name, bc.image_file
FROM user_badge_endorsement_request AS uber
JOIN user_badge AS ub ON ub.id= uber.user_badge_id
JOIN user AS u ON u.id = ub.user_id
JOIN user AS issuer ON uber.issuer_id = issuer.id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE uber.status = 'pending' AND u.id = :id

--name: select-endorsement-request-owner
SELECT uber.issuer_id, u.id FROM user_badge_endorsement_request AS uber
JOIN user_badge AS ub ON ub.id= uber.user_badge_id
JOIN user AS u ON u.id = ub.user_id
WHERE uber.id = :id

--name: update-endorsement-request-status!
UPDATE user_badge_endorsement_request SET status = :status, mtime = UNIX_TIMESTAMP() WHERE id = :id

--name: delete-endorsement-request!
DELETE FROM user_badge_endorsement_request WHERE id = :id

--name: select-user-received-endorsement-status
SELECT status FROM user_badge_endorsement WHERE issuer_id = :issuer_id AND user_badge_id = :id AND status != 'declined'

--name: select-user-endorsement-request-status
SELECT status FROM user_badge_endorsement_request WHERE issuer_id = :issuer_id AND user_badge_id = :id AND status = 'pending'

--name: select-user-badge-endorsement-request-by-issuer-id
SELECT id, status FROM user_badge_endorsement_request WHERE user_badge_id = :user_badge_id AND  issuer_id = :issuer_id AND status = 'pending'

--name: pending-user-badge-endorsement-count
--get user badge's pending endorsement
SELECT COUNT(id) AS count FROM user_badge_endorsement WHERE user_badge_id = :id AND status = 'pending';

--name: pending-user-badge-endorsement-count-multi
--get user badge's pending endorsement
SELECT COUNT(ube.id) AS count, ube.user_badge_id FROM user_badge_endorsement AS ube WHERE ube.user_badge_id IN (:user_badge_ids) AND ube.status = 'pending';

--name: delete-user-badge-endorsement-requests!
DELETE FROM user_badge_endorsement_request WHERE user_badge_id = :id

--name: select-request-by-request-id
SELECT user_badge_id, content FROM user_badge_endorsement_request WHERE id = :id

--name: sent-pending-requests-by-badge-id
SELECT uber.id, uber.user_badge_id, uber.status, uber.content, uber.mtime, uber.issuer_id AS user_id, issuer.profile_picture, issuer.first_name, issuer.last_name
FROM user_badge_endorsement_request AS uber
JOIN user_badge AS ub ON ub.id = uber.user_badge_id
LEFT JOIN user AS issuer ON issuer.id = uber.issuer_id
WHERE uber.user_badge_id = :id AND uber.status = 'pending'
ORDER BY uber.mtime DESC

--name:select-endorsement-event-info-by-endorsement-id
-- return given endorsement event information
SELECT ube.id, ube.user_badge_id, ube.content, bc.name FROM social_event AS se
INNER JOIN user_badge_endorsement AS ube ON ube.id = se.object AND se.verb = 'endorse_badge'
INNER JOIN user_badge AS ub ON ub.id= ube.user_badge_id
INNER JOIN badge AS badge ON (badge.id = ub.badge_id)
INNER JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
INNER JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE se.object = :id

--name: select-accepted-badge-endorsements
SELECT id FROM user_badge_endorsement AS ube WHERE ube.user_badge_id = :id AND ube.status = 'accepted'

--name: insert-user-badge-endorsement<!
INSERT INTO user_badge_endorsement (external_id,user_badge_id, issuer_id, issuer_name, issuer_url, content, status, ctime, mtime)
VALUES (:external_id, :user_badge_id, :issuer_id, :issuer_name, :issuer_url, :content, 'pending', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: delete-user-badge-endorsement!
DELETE FROM user_badge_endorsement WHERE id = :id

--name: select-endorsement-owner
SELECT issuer_id FROM user_badge_endorsement WHERE id = :id

--name: select-user-badge-endorsements
SELECT ube.id, ube.user_badge_id, ube.issuer_id, ube.issuer_name, ube.issuer_url, ube.content, ube.status, ube.mtime,u.profile_picture, u.profile_visibility
FROM user_badge_endorsement AS ube
LEFT JOIN user AS u on u.id = ube.issuer_id
WHERE user_badge_id = :user_badge_id
ORDER BY ube.mtime DESC

--name: update-endorsement-status!
UPDATE user_badge_endorsement SET status = :status WHERE id = :id

--name: select-user-badge-endorser
SELECT issuer_id FROM user_badge_endorsement WHERE user_badge_id = :user_badge_id AND issuer_id = :issuer_id

--name: select-pending-endorsements
SELECT ube.id, ube.user_badge_id, ube.issuer_id, ube.issuer_name, ube.issuer_url, ube.content, ube.ctime,
endorser.profile_picture, bc.name, bc.image_file, bc.description
FROM user_badge_endorsement AS ube
LEFT JOIN user AS endorser ON endorser.id = ube.issuer_id
JOIN user_badge AS ub ON ub.id = ube.user_badge_id
JOIN user AS recepient ON  ub.user_id = recepient.id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE recepient.id = :user_id AND ube.status = 'pending'
ORDER BY ube.mtime DESC

--name: update-user-badge-endorsement!
UPDATE user_badge_endorsement SET status = 'pending', content = :content, mtime = UNIX_TIMESTAMP()
WHERE id = :id

--name: select-given-endorsements
SELECT ube.id, ube.user_badge_id, ube.content, ube.mtime, bc.name, bc.description, bc.image_file, u.id AS endorsee_id, u.profile_picture, u.first_name, u.last_name, ube.status
FROM user_badge_endorsement AS ube
JOIN user_badge AS ub ON ub.id=ube.user_badge_id
JOIN user AS u on u.id = ub.user_id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE ube.issuer_id = :user_id

--name: select-received-endorsements
SELECT ube.id, ube.user_badge_id, ube.issuer_id, ube.issuer_name, ube.issuer_url, ube.content, ube.status, ube.mtime,
endorser.profile_picture, bc.name, bc.image_file, bc.description
FROM user_badge_endorsement AS ube
LEFT JOIN user AS endorser ON endorser.id = ube.issuer_id
JOIN user_badge AS ub ON ub.id = ube.user_badge_id
JOIN user AS recepient ON  ub.user_id = recepient.id
JOIN badge AS badge ON (badge.id = ub.badge_id)
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
WHERE recepient.id = :user_id
ORDER BY ube.mtime DESC

--name: delete-user-badge-endorsements!
DELETE FROM user_badge_endorsement WHERE user_badge_id = :id

--name: select-endorsement-by-issuerid-and-badgeid
SELECT id from user_badge_endorsement WHERE user_badge_id = :id AND issuer_id = :user_id AND status != 'declined';
