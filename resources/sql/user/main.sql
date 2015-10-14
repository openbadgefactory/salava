-- name: select-user-email-addresses
-- get user's badges
SELECT email FROM user_email
       WHERE user_id = :userid