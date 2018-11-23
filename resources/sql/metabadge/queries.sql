--name: select-user-badge-by-assertion-url
SELECT id, issued_on, status, deleted FROM user_badge AS ub WHERE ub.assertion_url = :assertion_url ORDER BY ctime DESC
