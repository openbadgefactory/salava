--name: select-user-badges-all
-- get user's badges
SELECT ub.id, ub.badge_id, bc.name, bc.description, bc.image_file, b.default_language_code,
    ub.issued_on, ub.expires_on, ub.revoked, ub.visibility, ub.ctime, ub.mtime, ub.status,
    ic.id AS issuer_id, ic.name AS issuer_name, ic.email AS issuer_email, ic.url AS issuer_url, ic.image_file AS issuer_image_file, ic.description AS issuer_description,
    cc.id AS creator_id, cc.name AS creator_name, cc.email AS creator_email, cc.url AS creator_url, cc.image_file AS creator_image_file, cc.description AS creator_description,
    GROUP_CONCAT(bt.tag) AS tags
FROM user_badge ub
INNER JOIN badge b ON ub.badge_id = b.id
INNER JOIN badge_badge_content bb ON b.id = bb.badge_id
INNER JOIN badge_content bc ON bb.badge_content_id = bc.id
INNER JOIN badge_issuer_content bi ON b.id = bi.badge_id
INNER JOIN issuer_content ic ON bi.issuer_content_id = ic.id
LEFT JOIN badge_tag bt ON ub.id = bt.user_badge_id
LEFT JOIN badge_creator_content bcc ON b.id = bcc.badge_id
LEFT JOIN creator_content cc ON bcc.creator_content_id = cc.id
WHERE ub.user_id = :user_id
    AND ub.deleted = 0 AND ub.status != 'declined'
    AND bc.language_code = b.default_language_code
    AND ic.language_code = b.default_language_code
GROUP BY ub.id
ORDER BY ub.id;


--name: select-user-badge
-- get user's badge by id
SELECT ub.id, ub.badge_id, bc.name, bc.description, bc.image_file, b.default_language_code,
    ub.issued_on, ub.expires_on, ub.revoked, ub.visibility, ub.ctime, ub.mtime, ub.status,
    ub.gallery_id, ub.assertion_url, ub.assertion_jws, ub.show_recipient_name,
    u.first_name, u.last_name, u.id AS owner_id,
    ic.id AS issuer_id, ic.name AS issuer_name, ic.email AS issuer_email, ic.url AS issuer_url, ic.image_file AS issuer_image_file, ic.description AS issuer_description,
    cc.id AS creator_id, cc.name AS creator_name, cc.email AS creator_email, cc.url AS creator_url, cc.image_file AS creator_image_file, cc.description AS creator_description,
    GROUP_CONCAT(bt.tag) AS tags
FROM user_badge ub
INNER JOIN user u ON ub.user_id = u.id
INNER JOIN badge b ON ub.badge_id = b.id
INNER JOIN badge_badge_content bb ON b.id = bb.badge_id
INNER JOIN badge_issuer_content bi ON b.id = bi.badge_id
INNER JOIN badge_content bc ON bb.badge_content_id = bc.id
INNER JOIN issuer_content ic ON bi.issuer_content_id = ic.id
LEFT JOIN badge_tag bt ON ub.id = bt.user_badge_id
LEFT JOIN badge_creator_content bcc ON b.id = bcc.badge_id
LEFT JOIN creator_content cc ON bcc.creator_content_id = cc.id
WHERE ub.user_id = :user_id AND ub.id = :id
    AND ub.deleted = 0 AND ub.status != 'declined'
    AND bc.language_code = b.default_language_code
    AND ic.language_code = b.default_language_code;


--name: select-badge-detail-count
--get badge endorsement, evicence and congratulation counts by id
SELECT
(COUNT(DISTINCT ube.id) + COUNT(DISTINCT be.endorsement_content_id) + COUNT(DISTINCT ie.endorsement_content_id)) AS endorsement_count,
COUNT(DISTINCT ue.id) AS evidence_count,
COUNT(DISTINCT c.user_id) AS congratulation_count
FROM user_badge ub
LEFT JOIN badge_endorsement_content be ON be.badge_id = ub.badge_id
LEFT JOIN badge_issuer_content bi ON bi.badge_id = ub.badge_id
LEFT JOIN issuer_endorsement_content ie ON bi.issuer_content_id = ie.issuer_content_id
LEFT JOIN user_badge_endorsement ube ON (ube.user_badge_id = ub.id AND ube.status = 'accepted')
LEFT JOIN user_badge_evidence ue ON ue.user_badge_id = ub.id
LEFT JOIN badge_congratulation c ON c.user_badge_id = ub.id
WHERE ub.id = :id
GROUP BY ub.id;

--name: select-badge-content
--get multilingual badge content by id
SELECT bc.name, bc.language_code, bc.description, cc.markdown_text AS criteria_content, cc.url AS criteria_url
FROM badge b
INNER JOIN badge_badge_content bbc ON bbc.badge_id = b.id
INNER JOIN badge_content bc ON bc.id = bbc.badge_content_id
INNER JOIN badge_criteria_content bcc ON bcc.badge_id = b.id
INNER JOIN criteria_content cc ON (cc.id = bcc.criteria_content_id AND bc.language_code = cc.language_code)
WHERE b.id = :badge_id;


--name: select-badge-content-alignments
--get badge alignments for one language
SELECT a.name, a.url, a.description FROM badge_content_alignment a
INNER JOIN badge_content bc ON a.badge_content_id = bc.id
INNER JOIN badge_badge_content bbc ON bbc.badge_content_id = bc.id
WHERE bbc.badge_id = :badge_id AND bc.language_code = :language
ORDER BY a.name;

--name: select-user-badge-endorsements
--get badge class endorsements
SELECT e.id, e.content, e.issued_on,
    ei.id AS issuer_id, ei.name AS issuer_name, ei.email AS issuer_email,
    ei.url AS issuer_url, ei.image_file AS issuer_image_file, ei.description AS issuer_description
FROM endorsement_content e
INNER JOIN badge_endorsement_content bec ON bec.endorsement_content_id = e.id
INNER JOIN issuer_content ei ON e.issuer_content_id = ei.id
INNER JOIN user_badge ub ON bec.badge_id = ub.badge_id
WHERE ub.id = :id AND ub.user_id = :user_id
ORDER BY e.issued_on;

--name: select-user-issuer-endorsements
--get issuer class endorsements
SELECT e.id, e.content, e.issued_on,
    ei.id AS issuer_id, ei.name AS issuer_name, ei.email AS issuer_email,
    ei.url AS issuer_url, ei.image_file AS issuer_image_file, ei.description AS issuer_description
FROM endorsement_content e
INNER JOIN issuer_content ei ON e.issuer_content_id = ei.id
INNER JOIN issuer_endorsement_content iec ON iec.endorsement_content_id = e.id
INNER JOIN badge_issuer_content bic ON iec.issuer_content_id = bic.issuer_content_id
INNER JOIN user_badge ub ON bic.badge_id = ub.badge_id
WHERE ub.id = :id AND ub.user_id = :user_id
ORDER BY e.issued_on;

--name: select-user-endorsements
--get user endorsements for a badge
SELECT e.id, e.content, e.mtime AS issued_on, e.issuer_id, e.issuer_name, e.issuer_url,
    u.profile_picture AS issuer_image_file
FROM user_badge_endorsement e
INNER JOIN user u ON e.issuer_id = u.id
INNER JOIN user_badge ub ON e.user_badge_id = ub.id
WHERE ub.id = :id AND ub.user_id = :user_id AND e.status = 'accepted'
UNION
SELECT e.id, e.content, e.mtime AS issued_on, e.issuer_id, e.issuer_name, e.issuer_url,
    u.image_file AS issuer_image_file
FROM user_badge_endorsement_ext e
INNER JOIN user_ext u ON e.issuer_id = u.id
INNER JOIN user_badge ub ON e.user_badge_id = ub.id
WHERE ub.id = :id AND ub.user_id = :user_id AND e.status = 'accepted'
ORDER BY issued_on;

--name: select-user-badge-congratulations
--get all users who congratulated another user from specific badge
SELECT u.id, u.first_name, u.last_name, u.profile_picture, bc.ctime FROM user u
INNER JOIN badge_congratulation AS bc ON u.id = bc.user_id
INNER JOIN user_badge ub ON bc.user_badge_id = ub.id
WHERE ub.id = :user_badge_id AND ub.user_id = :owner;


--name: select-gallery-badge
-- get gallery badge by gallery.id
SELECT g.id, g.badge_id, bc.name, bc.description, bc.image_file, b.default_language_code,
    ic.id AS issuer_id, ic.name AS issuer_name, ic.email AS issuer_email, ic.url AS issuer_url, ic.image_file AS issuer_image_file, ic.description AS issuer_description,
    cc.id AS creator_id, cc.name AS creator_name, cc.email AS creator_email, cc.url AS creator_url, cc.image_file AS creator_image_file, cc.description AS creator_description,
    (SELECT COUNT(*) FROM user_badge WHERE deleted = 0 AND status != 'declined' AND visibility != 'private' AND revoked = 0 AND gallery_id = :gallery_id) AS recipient_count
FROM gallery g
INNER JOIN user_badge ub
INNER JOIN badge b ON g.badge_id = b.id
INNER JOIN badge_badge_content bb ON b.id = bb.badge_id
INNER JOIN badge_issuer_content bi ON b.id = bi.badge_id
INNER JOIN badge_content bc ON bb.badge_content_id = bc.id
INNER JOIN issuer_content ic ON bi.issuer_content_id = ic.id
LEFT JOIN badge_tag bt ON ub.id = bt.user_badge_id
LEFT JOIN badge_creator_content bcc ON b.id = bcc.badge_id
LEFT JOIN creator_content cc ON bcc.creator_content_id = cc.id
WHERE g.id = :gallery_id AND g.badge_id = :badge_id
    AND ub.deleted = 0 AND ub.revoked = 0 AND ub.status != 'declined' AND ub.visibility != 'private'
    AND bc.language_code = b.default_language_code
    AND ic.language_code = b.default_language_code
ORDER BY ub.ctime DESC
LIMIT 1;
