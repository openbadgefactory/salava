
-- name: select-user-pages
-- get user's badges
SELECT id, name, description, theme, visibility, password, visible_after, visible_before, ctime, mtime FROM page
       WHERE user_id = :user_id

-- name: insert-empty-page<!
-- create a new empty page
INSERT INTO page (user_id, name, visibility, ctime, mtime) VALUES (:user_id, :name, "private", UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

-- name: select-page
-- get page
SELECT p.id, name, description, theme, visibility, password, visible_after, visible_before, p.ctime, p.mtime, user_id, u.first_name, u.last_name FROM page AS p
       JOIN user AS u ON u.id = p.user_id
       WHERE p.id = :id

-- name: select-pages-badge-blocks
SELECT pb.id, "badge" AS type, page_id, block_order, pb.badge_id, format, b.issued_on, bc.name, bc.description, bc.image_file, b.criteria_url, bc.criteria_markdown, ic.name AS issuer_name, ic.url AS issuer_url, ic.email AS issuer_email FROM page_block_badge AS pb
       JOIN badge AS b ON pb.badge_id = b.id
       JOIN badge_content AS bc ON b.badge_content_id = bc.id
       JOIN issuer_content AS ic ON b.issuer_content_id = ic.id
       WHERE page_id = :page_id

-- name: select-pages-file-blocks
SELECT id, "file" AS type, page_id, block_order, file_id FROM page_block_file
       WHERE page_id = :page_id

-- name: select-pages-heading-blocks
SELECT id, "heading" AS type, page_id, block_order, size, content  FROM page_block_heading
       WHERE page_id = :page_id

-- name: select-pages-html-blocks
SELECT id, "html" AS type, page_id, block_order, content FROM page_block_html
       WHERE page_id = :page_id
