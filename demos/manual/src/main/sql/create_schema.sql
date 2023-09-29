create user if not exists scott password 'tiger';
alter user scott admin true;

create schema store;

create table store.customer (
  id varchar(36) not null,
  first_name varchar(40) not null,
  last_name varchar(40) not null,
  email varchar(100),
  active boolean default true not null,
  constraint customer_pk primary key (id),
  constraint customer_email_uk unique (email)
);

create table store.address (
  id identity not null,
  street varchar(120) not null,
  city varchar(50) not null,
  valid boolean default true not null,
  constraint address_pk primary key (id),
  constraint address_uk unique (street, city)
);

create table store.customer_address (
  id identity not null,
  customer_id varchar(36) not null,
  address_id integer not null,
  constraint customer_address_pk primary key (id),
  constraint customer_address_uk unique (customer_id, address_id)
);

insert into store.customer(id, first_name, last_name, email)
values ('ff60ebc3-bae0-4c0f-b094-4129edd3665a', 'John', 'Doe', 'doe@doe.net');

insert into store.address(street, city)
values ('Elm Street 123', 'Syracuse');

insert into store.customer_address(customer_id, address_id)
values ('ff60ebc3-bae0-4c0f-b094-4129edd3665a', 1);

commit;