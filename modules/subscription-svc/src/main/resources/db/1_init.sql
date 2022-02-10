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
    address_country varchar(50),
    created_at      timestamp   NOT NULL,
    modified_at     timestamp
);

CREATE INDEX user_id_idx ON subscriptions (user_id);


CREATE TABLE subscription_events
(
    id         varchar(40) PRIMARY KEY,
    entity_id  varchar(40)  NOT NULL,
    type       varchar(100) NOT NULL,
    data       bytea        NOT NULL,
    created_at timestamp    NOT NULL
);

CREATE INDEX entity_id_idx ON subscription_events (entity_id);