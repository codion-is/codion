create user if not exists scott password 'tiger';
alter user scott admin true;

CREATE SCHEMA petclinic;

CREATE TABLE petclinic.vet (
  id         INTEGER IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR(30)
);
CREATE INDEX vet_last_name ON petclinic.vet (last_name);

CREATE TABLE petclinic.specialty (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX specialty_name ON petclinic.specialty (name);

CREATE TABLE petclinic.vet_specialty (
  vet       INTEGER NOT NULL,
  specialty INTEGER NOT NULL
);
ALTER TABLE petclinic.vet_specialty ADD CONSTRAINT fk_vet_specialty_vet FOREIGN KEY (vet) REFERENCES petclinic.vet (id);
ALTER TABLE petclinic.vet_specialty ADD CONSTRAINT fk_vet_specialty_specialty FOREIGN KEY (specialty) REFERENCES petclinic.specialty (id);

CREATE TABLE petclinic.pet_type (
  id   INTEGER IDENTITY PRIMARY KEY,
  name VARCHAR(80)
);
CREATE INDEX pet_type_name ON petclinic.pet_type (name);

CREATE TABLE petclinic.owner (
  id         INTEGER IDENTITY PRIMARY KEY,
  first_name VARCHAR(30),
  last_name  VARCHAR_IGNORECASE(30),
  address    VARCHAR(255),
  city       VARCHAR(80),
  telephone  VARCHAR(20)
);
CREATE INDEX owners_last_name ON petclinic.owner (last_name);

CREATE TABLE petclinic.pet (
  id         INTEGER IDENTITY PRIMARY KEY,
  name       VARCHAR(30),
  birth_date DATE,
  type_id    INTEGER NOT NULL,
  owner_id   INTEGER NOT NULL
);
ALTER TABLE petclinic.pet ADD CONSTRAINT fk_pet_owners FOREIGN KEY (owner_id) REFERENCES owner (id);
ALTER TABLE petclinic.pet ADD CONSTRAINT fk_pet_pet_type FOREIGN KEY (type_id) REFERENCES pet_type (id);
CREATE INDEX pets_name ON petclinic.pet (name);

CREATE TABLE petclinic.visit (
  id          INTEGER IDENTITY PRIMARY KEY,
  pet_id      INTEGER NOT NULL,
  date  DATE,
  description VARCHAR(255)
);
ALTER TABLE petclinic.visit ADD CONSTRAINT fk_visit_pets FOREIGN KEY (pet_id) REFERENCES pet (id);
CREATE INDEX visit_pet_id ON petclinic.visit (pet_id);

INSERT INTO petclinic.vet VALUES (1, 'James', 'Carter');
INSERT INTO petclinic.vet VALUES (2, 'Helen', 'Leary');
INSERT INTO petclinic.vet VALUES (3, 'Linda', 'Douglas');
INSERT INTO petclinic.vet VALUES (4, 'Rafael', 'Ortega');
INSERT INTO petclinic.vet VALUES (5, 'Henry', 'Stevens');
INSERT INTO petclinic.vet VALUES (6, 'Sharon', 'Jenkins');

INSERT INTO petclinic.specialty VALUES (1, 'radiology');
INSERT INTO petclinic.specialty VALUES (2, 'surgery');
INSERT INTO petclinic.specialty VALUES (3, 'dentistry');

INSERT INTO petclinic.vet_specialty VALUES (2, 1);
INSERT INTO petclinic.vet_specialty VALUES (3, 2);
INSERT INTO petclinic.vet_specialty VALUES (3, 3);
INSERT INTO petclinic.vet_specialty VALUES (4, 2);
INSERT INTO petclinic.vet_specialty VALUES (5, 1);

INSERT INTO petclinic.pet_type VALUES (1, 'cat');
INSERT INTO petclinic.pet_type VALUES (2, 'dog');
INSERT INTO petclinic.pet_type VALUES (3, 'lizard');
INSERT INTO petclinic.pet_type VALUES (4, 'snake');
INSERT INTO petclinic.pet_type VALUES (5, 'bird');
INSERT INTO petclinic.pet_type VALUES (6, 'hamster');

INSERT INTO petclinic.owner VALUES (1, 'George', 'Franklin', '110 W. Liberty St.', 'Madison', '6085551023');
INSERT INTO petclinic.owner VALUES (2, 'Betty', 'Davis', '638 Cardinal Ave.', 'Sun Prairie', '6085551749');
INSERT INTO petclinic.owner VALUES (3, 'Eduardo', 'Rodriquez', '2693 Commerce St.', 'McFarland', '6085558763');
INSERT INTO petclinic.owner VALUES (4, 'Harold', 'Davis', '563 Friendly St.', 'Windsor', '6085553198');
INSERT INTO petclinic.owner VALUES (5, 'Peter', 'McTavish', '2387 S. Fair Way', 'Madison', '6085552765');
INSERT INTO petclinic.owner VALUES (6, 'Jean', 'Coleman', '105 N. Lake St.', 'Monona', '6085552654');
INSERT INTO petclinic.owner VALUES (7, 'Jeff', 'Black', '1450 Oak Blvd.', 'Monona', '6085555387');
INSERT INTO petclinic.owner VALUES (8, 'Maria', 'Escobito', '345 Maple St.', 'Madison', '6085557683');
INSERT INTO petclinic.owner VALUES (9, 'David', 'Schroeder', '2749 Blackhawk Trail', 'Madison', '6085559435');
INSERT INTO petclinic.owner VALUES (10, 'Carlos', 'Estaban', '2335 Independence La.', 'Waunakee', '6085555487');

INSERT INTO petclinic.pet VALUES (1, 'Leo', '2010-09-07', 1, 1);
INSERT INTO petclinic.pet VALUES (2, 'Basil', '2012-08-06', 6, 2);
INSERT INTO petclinic.pet VALUES (3, 'Rosy', '2011-04-17', 2, 3);
INSERT INTO petclinic.pet VALUES (4, 'Jewel', '2010-03-07', 2, 3);
INSERT INTO petclinic.pet VALUES (5, 'Iggy', '2010-11-30', 3, 4);
INSERT INTO petclinic.pet VALUES (6, 'George', '2010-01-20', 4, 5);
INSERT INTO petclinic.pet VALUES (7, 'Samantha', '2012-09-04', 1, 6);
INSERT INTO petclinic.pet VALUES (8, 'Max', '2012-09-04', 1, 6);
INSERT INTO petclinic.pet VALUES (9, 'Lucky', '2011-08-06', 5, 7);
INSERT INTO petclinic.pet VALUES (10, 'Mulligan', '2007-02-24', 2, 8);
INSERT INTO petclinic.pet VALUES (11, 'Freddy', '2010-03-09', 5, 9);
INSERT INTO petclinic.pet VALUES (12, 'Lucky', '2010-06-24', 2, 10);
INSERT INTO petclinic.pet VALUES (13, 'Sly', '2012-06-08', 1, 10);

INSERT INTO petclinic.visit VALUES (1, 7, '2013-01-01', 'rabies shot');
INSERT INTO petclinic.visit VALUES (2, 8, '2013-01-02', 'rabies shot');
INSERT INTO petclinic.visit VALUES (3, 8, '2013-01-03', 'neutered');
INSERT INTO petclinic.visit VALUES (4, 7, '2013-01-04', 'spayed');