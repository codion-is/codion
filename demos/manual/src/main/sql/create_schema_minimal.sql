create user if not exists scott password 'tiger';
alter user scott admin true;

create schema store;

create table store.customer (
  id identity not null,
  first_name varchar(40) not null,
  last_name varchar(40) not null,
  email varchar(100),
  active boolean default true not null,
  constraint customer_pk primary key (id),
  constraint customer_email_uk unique (email)
);

create table store.address (
  id identity not null,
  customer_id integer not null,
  street varchar(100) not null,
  city varchar(50) not null,
  constraint address_pk primary key (id),
  constraint customer_fk foreign key (customer_id) references store.customer (id)
);

insert into store.customer(first_name, last_name, email)
values ('John', 'Doe', 'john@doe.net');

insert into store.address(customer_id, street, city)
values (1, 'Elm Street', 'Syracuse');

insert into store.address(customer_id, street, city)
values (1, 'Wall Street', 'New York');

insert into store.customer(first_name, last_name, email)
values ('Jane', 'Doe', 'jane@doe.net');

insert into store.address(customer_id, street, city)
values (2, 'Elm Street', 'Syracuse');

insert into store.customer(first_name, last_name, email)
values ('Andy', 'Taylor', null);

insert into store.address(customer_id, street, city)
values (3, 'Bloom Street', 'Springfield');

insert into store.customer(first_name, last_name, email)
values ('Carol', 'Richards', null);

insert into store.address(customer_id, street, city)
values (4, 'Carrot Street', 'Oakfield');

insert into store.address(customer_id, street, city)
values (4, 'Raspberry Street', 'New York');

insert into store.customer(first_name, last_name, email)
values ('Paul', 'Roberts', 'paul@doe.net');

insert into store.address(customer_id, street, city)
values (5, 'Raspberry Street', 'New York');

insert into store.customer(first_name, last_name, email)
values ('Maggie', 'Allen', 'maggie@doe.net');

insert into store.address(customer_id, street, city)
values (6, 'Raspberry Street', 'New York');

commit;