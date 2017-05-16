create table vocabularies (
    id SERIAL UNIQUE not null PRIMARY KEY,
    isVisible boolean,
    participant_id int REFERENCES participants (id) ON DELETE CASCADE,
    date timestamp not null
);
