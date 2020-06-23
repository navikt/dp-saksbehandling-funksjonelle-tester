CREATE TABLE person
(
    id  BIGSERIAL,
    fnr VARCHAR(32) NOT NULL,
    PRIMARY KEY (id)
);

create index "index_person_fnr" on person using btree (fnr);

CREATE TABLE melding
(
    id  BIGSERIAL,
    fnr VARCHAR(32) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX "index_melding_fnr" ON melding USING btree (fnr);
