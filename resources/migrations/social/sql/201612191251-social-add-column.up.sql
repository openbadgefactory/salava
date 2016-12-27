ALTER TABLE social_event MODIFY COLUMN verb enum('message','follow','publish','delete_message', 'ticket')
--;;
ALTER TABLE social_event MODIFY COLUMN type enum('badge','user','page','admin')
