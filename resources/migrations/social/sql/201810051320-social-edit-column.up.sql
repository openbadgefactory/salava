ALTER TABLE social_event MODIFY COLUMN verb enum('message','follow','publish','delete_message', 'ticket', 'congratulate', 'modify', 'unpublish', 'advertise');
--;;
ALTER TABLE social_event modify COLUMN type enum ('badge','user','page','admin', 'advert');
