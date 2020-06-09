--name: total-user-count
SELECT COUNT(DISTINCT us.user_id) AS count FROM user_space us
JOIN user u ON u.id = us.user_id
WHERE us.space_id = :id AND u.deleted = 0;

--name: activated-user-count
SELECT COUNT(DISTINCT us.user_id) AS count FROM user_space us
JOIN user u ON u.id = us.user_id
WHERE us.space_id = :id AND u.deleted = 0 AND u.activated = 1

--name: not-activated-user-count
SELECT COUNT(DISTINCT us.user_id) AS count FROM user_space us
JOIN user u ON u.id = us.user_id
WHERE us.space_id = :id AND u.deleted = 0 AND u.activated = 0

--name: count-registered-users-after-date
SELECT COUNT(DISTINCT us.user_id) AS count FROM user_space us
JOIN user u ON u.id = us.user_id
WHERE us.space_id = :id AND u.deleted = 0 AND u.activated = 1 AND us.ctime > :time;

--name: internal-user-count
SELECT COUNT(DISTINCT us.user_id) AS count FROM user_space us
JOIN user u ON u.id = us.user_id
WHERE us.space_id = :id AND u.deleted = 0 AND u.activated = 1 AND u.profile_visibility = "internal"

--name: public-user-count
SELECT COUNT(DISTINCT us.user_id) AS count FROM user_space us
JOIN user u ON u.id = us.user_id
WHERE us.space_id = :id AND u.deleted = 0 AND u.activated = 1 AND u.profile_visibility = "public"

--name: count-all-badges
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND us.space_id = :id;

--name: count-pending-badges
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status= "pending" AND us.space_id = :id;

--name: count-accepted-badges
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status= "accepted" AND us.space_id = :id;

--name: count-declined-badges
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status= "declined" AND us.space_id = :id;

--name: count-all-badges-after-date
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status= "pending" AND us.ctime > :time AND us.space_id = :id;

--name: count-private-badges
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status= "accepted" AND ub.visibility = "private" AND us.space_id = :id;

--name: count-public-badges
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status= "accepted" AND ub.visibility = "public" AND us.space_id = :id;

--name: count-internal-badges
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status= "accepted" AND ub.visibility = "internal" AND us.space_id = :id;

--name: count-badges-issued-from-url
SELECT COUNT(DISTINCT ub.id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0  AND ub.revoked = 0 AND ub.status != "declined" AND us.space_id = :id AND ub.assertion_url LIKE :url;

--name: count-all-pages
SELECT COUNT(DISTINCT p.id) AS count FROM page p
JOIN user_space us ON us.user_id = p.user_id
WHERE p.deleted = 0 AND us.space_id = :id;

--name: count-all-pages-after-date
SELECT COUNT(DISTINCT p.id) AS count FROM page p
JOIN user_space us ON us.user_id = p.user_id
WHERE p.deleted = 0 AND us.space_id = :id AND us.ctime > :time;

--name: count-private-pages
SELECT COUNT(DISTINCT p.id) AS count FROM page p
JOIN user_space us ON us.user_id = p.user_id
WHERE p.deleted = 0 AND p.visibility = "private" AND us.space_id = :id;

--name: count-public-pages
SELECT COUNT(DISTINCT p.id) AS count FROM page p
JOIN user_space us ON us.user_id = p.user_id
WHERE p.deleted = 0 AND p.visibility = "public" AND us.space_id = :id;

--name: count-internal-pages
SELECT COUNT(DISTINCT p.id) AS count FROM page p
JOIN user_space us ON us.user_id = p.user_id
WHERE p.deleted = 0 AND p.visibility = "internal" AND us.space_id = :id;

--name: created-badges-count
SELECT COUNT(DISTINCT s.id) AS count FROM selfie_badge s
JOIN user_space us ON us.user_id = s.creator_id
WHERE s.deleted = 0 AND us.space_id = :id

--name: issued-badges-count
SELECT COUNT(DISTINCT id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0 AND ub.revoked = 0 AND ub.status != 'declined' AND ub.selfie_id IS NOT NULL;

--name: created-badges-count-after-date
SELECT COUNT(DISTINCT s.id) AS count FROM selfie_badge s
JOIN user_space us ON us.user_id = s.creator_id
WHERE s.deleted = 0 AND us.space_id = :id AND us.ctime > :time;

--name: issued-badges-count-after-date
SELECT COUNT(DISTINCT id) AS count FROM user_badge ub
JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.deleted = 0 AND ub.revoked = 0 AND ub.status != 'declined' AND ub.selfie_id IS NOT NULL AND us.ctime > :time;

--name: count-badge-issuers-after-date
SELECT COUNT(nih.id) AS count FROM user_badge ub
JOIN badge_issuer_content bic ON bic.badge_id = ub.badge_id
JOIN issuer_content ic ON bic.issuer_content_id = ic.id
JOIN new_issuer_history nih ON nih.issuer_content_id = ic.id
JOIN user_space us ON us.user_id = ub.user_id
WHERE us.ctime > :time AND us.space_id = :id

--name: count-badge-issuers
SELECT COUNT(DISTINCT ic.name) AS count FROM user_badge ub
JOIN badge_issuer_content bic ON bic.badge_id = ub.badge_id
JOIN issuer_content ic ON bic.issuer_content_id = ic.id
JOIN user_space us ON us.user_id = ub.user_id
WHERE us.space_id = :id;
