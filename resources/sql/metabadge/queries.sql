--name: select-user-badge-by-assertion-url
SELECT id, issued_on, status, deleted FROM user_badge AS ub WHERE ub.assertion_url = :assertion_url ORDER BY ctime DESC

--name all-metabadges
SELECT ub.id, ub.assertion_url FROM user_badge AS ub
LEFT JOIN user_badge_metabadge AS ubm ON ubm.user_badge_id = ub.id
WHERE ubm.meta_badge IS NOT NULL OR ubm.meta_badge_req IS NOT NULL
