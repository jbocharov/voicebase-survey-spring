ALTER TABLE responses ADD COLUMN
    participant_id int REFERENCES participants (id) ON DELETE CASCADE;
