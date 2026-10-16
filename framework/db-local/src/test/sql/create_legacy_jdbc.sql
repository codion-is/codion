create schema legacy;

create table legacy.area (
  id uuid primary key,
  name varchar(20)
);

create table legacy.visit (
  id uuid primary key,
  area_id uuid references legacy.area (id)
);
