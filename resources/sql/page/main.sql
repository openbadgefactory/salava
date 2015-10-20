
-- name: select-user-pages
-- get user's badges
SELECT id, name, description, theme, visibility, password, visible_after, visible_before, ctime, mtime FROM page
       WHERE user_id = :user_id

-- name: insert-empty-page<!
-- create a new empty page
INSERT INTO page (user_id, name, visibility, ctime, mtime) VALUES (:user_id, :name, "private", UNIX_TIMESTAMP(), UNIX_TIMESTAMP())