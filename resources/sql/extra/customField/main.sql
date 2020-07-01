--name: select-field-by-name
SELECT value FROM user_properties WHERE name = :name AND user_id = :user_id;

--name: insert-custom-field!
REPLACE INTO user_properties (user_id, name, value) VALUES (:user_id, :name, :value );

--name: select-organizations
SELECT id, alias, name FROM space
WHERE visibility != "private" AND status = "active" AND (valid_until IS NULL OR valid_until > UNIX_TIMESTAMP());
