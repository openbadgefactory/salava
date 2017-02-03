ALTER TABLE badge_advert ADD UNIQUE INDEX idx_badge_advert_remote (remote_url(99), remote_id(99), remote_issuer_id(99));
