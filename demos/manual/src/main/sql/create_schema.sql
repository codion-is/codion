create user if not exists scott password 'tiger';
alter user scott admin true;

create schema store;

create table store.address (
  id identity not null,
  street varchar(120) not null,
  city varchar(50) not null,
  constraint address_pk primary key (id);
);

create table store.customer (
  id varchar(36) not null,
  first_name varchar(40) not null,
  last_name varchar(40) not null,
  email varchar(100),
  address_id integer not null,
  boolean is_active default true not null,
  constraint customer_pk primary key (id),
  constraint address_fk foreign key (address_id) references store.address (id)
)