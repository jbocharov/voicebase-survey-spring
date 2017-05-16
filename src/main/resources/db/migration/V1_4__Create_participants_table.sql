create table participants (
    id SERIAL UNIQUE not null PRIMARY KEY,
    phone_number varchar(255) not null,
    unmasked_phone_number varchar(255) not null,
    isVisible boolean,
    date timestamp not null
);
