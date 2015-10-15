-- name: select-user-email-addresses
-- get user's email-addresses
SELECT email FROM user_email
       WHERE user_id = :userid

-- name: select-user-primary-email-addresses
-- get user's badges
SELECT email FROM user_email
       WHERE user_id = :userid AND primary_address = 1