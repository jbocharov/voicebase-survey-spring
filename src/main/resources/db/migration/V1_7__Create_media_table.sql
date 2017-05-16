create table media (
    id SERIAL UNIQUE not null PRIMARY KEY,
    voicebase_media_id varchar(255) not null,
    participant_id int REFERENCES participants (id) ON DELETE CASCADE,
    vocabulary_id int REFERENCES vocabularies (id) ON DELETE CASCADE,
    date timestamp not null
);
