--name: select-user-language
SELECT language FROM user WHERE id = :id

--name: total-user-count
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1;

--name: count-logged-users-after-date
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND last_login > :time;

--name: count-registered-users-after-date
SELECT COUNT(DISTINCT id) AS count FROM user WHERE activated = 1 AND ctime > :time;

--name: count-all-badges
SELECT COUNT(DISTINCT id) AS count FROM badge;

--name: count-all-badges-after-date
SELECT COUNT(DISTINCT id) AS count FROM badge WHERE ctime > :time;

--name: count-all-pages
SELECT COUNT(DISTINCT id) AS count FROM page;
