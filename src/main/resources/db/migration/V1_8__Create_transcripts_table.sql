create table transcripts (
    id SERIAL UNIQUE not null PRIMARY KEY,
    media_id int REFERENCES media (id) ON DELETE CASCADE,
    vocabulary_id int REFERENCES vocabularies (id) ON DELETE CASCADE,
    date timestamp not null
);
