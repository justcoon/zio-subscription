CREATE TABLE subscriptions
(
    id              varchar(40) PRIMARY KEY,
    user_id         varchar(40) NOT NULL,
    email           varchar(50) NOT NULL,
    address_street  varchar(50),
    address_number  varchar(20),
    address_zip     varchar(10),
    address_city    varchar(50),
    address_state   varchar(50),
    address_country varchar(50)
);

CREATE INDEX user_id_idx ON subscriptions (user_id);