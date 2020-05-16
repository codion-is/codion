create user if not exists scott password 'tiger';
alter user scott admin true;

create schema store;

create table store.customer (
  id identity not null,
  first_name varchar(40) not null,
  last_name varchar(40) not null,
  email varchar(100),
  is_active boolean default true not null,
  constraint customer_pk primary key (id),
  constraint customer_email_uk unique (email)
);

insert into store.customer(first_name, last_name, email)
values ('John', 'Doe', 'john@doe.net');

insert into store.customer(first_name, last_name, email)
values ('Jane', 'Doe', 'jane@doe.net');

insert into store.customer(first_name, last_name, email)
values ('Andy', 'Taylor', null);

insert into store.customer(first_name, last_name, email)
values ('Carol', 'Richards', null);

insert into store.customer(first_name, last_name, email)
values ('Paul', 'Roberts', 'paul@doe.net');

insert into store.customer(first_name, last_name, email)
values ('Maggie', 'Allen', 'maggie@doe.net');

commit;