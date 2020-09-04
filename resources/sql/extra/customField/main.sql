--name: select-field-by-name
SELECT value FROM user_properties WHERE name = :name AND user_id = :user_id;

--name: insert-custom-field!
REPLACE INTO user_properties (user_id, name, value) VALUES (:user_id, :name, :value );

--name: select-organizations
SELECT id, alias, name FROM space
WHERE visibility != "private" AND status = "active" AND (valid_until IS NULL OR valid_until > UNIX_TIMESTAMP());

--name: select-custom-field-organizations
SELECT id, name FROM custom_org_list ORDER BY ctime DESC

--name: insert-custom-field-org!
INSERT INTO custom_org_list (name, ctime) VALUES (:name, UNIX_TIMESTAMP());

--name: delete-custom-field-org!
DELETE FROM custom_org_list WHERE id = :id

--name: select-org-name-by-id
SELECT name from custom_org_list WHERE id = :id

--name: delete-user-organization-property!
DELETE FROM user_properties WHERE name = "organization" AND value = :value
