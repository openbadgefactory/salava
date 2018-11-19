--name: select-user-badge-id-by-assertion-url
SELECT id FROM user_badge AS ub WHERE ub.assertion_url = :assertion_url AND ub.status != 'declined' ORDER BY ctime DESC
