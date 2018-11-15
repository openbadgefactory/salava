--name: select-user-badge-id-by-assertion-url
SELECT id FROM user_badge WHERE assertion_url = :assertion_url ORDER BY ctime DESC
