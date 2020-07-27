--name: select-firebase-tokens-by-emails
SELECT DISTINCT o.firebase_token, u.language FROM oauth2_token o
INNER JOIN user u ON o.user_id = u.id
INNER JOIN user_email e ON u.id = e.user_id
WHERE o.firebase_token IS NOT NULL AND e.verified = 1 AND e.email IN (:emails);
