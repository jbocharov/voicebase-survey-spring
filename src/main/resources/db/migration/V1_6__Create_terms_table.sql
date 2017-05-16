create table terms (
    id SERIAL UNIQUE not null PRIMARY KEY,
    term varchar(255) not null,
    sounds_like text,
    weight float,
    vocabulary_id int REFERENCES vocabularies (id) ON DELETE CASCADE,
    date timestamp not null
);
