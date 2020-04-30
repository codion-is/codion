create user if not exists scott password 'tiger';
alter user scott admin true;

create schema test;

create table test.test_table (
  id integer
);