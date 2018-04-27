-- name: select-public-badge
SELECT ub.id, bc.name FROM user_badge ub
INNER JOIN badge b ON ub.badge_id = b.id
INNER JOIN badge_badge_content bbc ON b.id = bbc.badge_id
INNER JOIN badge_content bc ON (bbc.badge_content_id = bc.id AND bc.language_code = b.default_language_code)
WHERE ub.id = :user_badge_id AND ub.deleted = 0 AND ub.revoked = 0
  AND ub.visibility = 'public' AND ub.status = 'accepted';
