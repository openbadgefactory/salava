--name: select-user-files
--Select all files owned by user
SELECT id, name, path, mime_type, size, ctime, mtime, GROUP_CONCAT(ft.tag) AS tags FROM user_file AS f
       LEFT JOIN user_file_tag AS ft ON f.id = ft.file_id WHERE user_id = :user_id
       GROUP BY id, name, path, mime_type, size, ctime, mtime

--name: replace-file-tag!
REPLACE INTO user_file_tag (file_id, tag)
       VALUES (:file_id, :tag)

--name: select-file-owner
--get file owner's user-id
SELECT user_id FROM user_file WHERE id = :id

--name: select-file-owner-and-path
SELECT user_id AS owner, path FROM user_file WHERE id = :id

--name: delete-file!
DELETE FROM user_file WHERE id = :id

--name: delete-files-block-file!
DELETE FROM page_block_files_has_file WHERE file_id = :file_id

--name: delete-file-tags!
DELETE FROM user_file_tag WHERE file_id = :file_id

--name: insert-file<!
INSERT INTO user_file (user_id, name, path, mime_type, size, ctime, mtime) VALUES (:user_id, :name, :path, :mime_type, :size, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: select-user-image-files
SELECT id, name, path, mime_type, size, ctime, mtime FROM user_file AS f
       WHERE user_id = :user_id AND mime_type LIKE 'image/%'

--name: select-file-usage
SELECT (SELECT COUNT(*) FROM user_file WHERE path = :path) AS file_count,
       (SELECT COUNT(*) FROM badge_content WHERE image_file = :path) AS badge_content_file_count,
       (SELECT COUNT(*) FROM issuer_content WHERE image_file = :path) AS issuer_content_file_count,
       (SELECT COUNT(*) FROM creator_content WHERE image_file = :path) AS creator_content_file_count,
       (SELECT COUNT(*) FROM user WHERE profile_picture = :path) AS profile_picture_file_count;

--name: select-profile-picture-path
SELECT profile_picture FROM user WHERE id = :id
