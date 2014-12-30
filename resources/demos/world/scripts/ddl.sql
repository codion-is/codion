create schema world;

create table world.country (
  code varchar(3) not null,
  name varchar(52) not null,
  continent varchar(20) not null,
  region varchar(26) not null,
  surfacearea decimal(10,2) not null,
  indepyear smallint,
  population int not null,
  lifeexpectancy decimal(3,1),
  gnp decimal(10,2),
  gnpold decimal(10,2),
  localname varchar(45) not null,
  governmentform varchar(45) not null,
  headofstate varchar(60),
  capital int,
  code2 varchar(2) not null,
  constraint country_pk primary key (code),
  constraint continent_chk check(continent in ('Asia','Europe','North America','Africa','Oceania','Antarctica','South America'))
);

create table world.city (
  id int not null,
  name varchar(35) not null,
  countrycode varchar(3) not null,
  district varchar(20) not null,
  population int not null,
  constraint city_pk primary key (id)
);

create sequence world.city_seq start with 4080;

create table world.countrylanguage (
  countrycode varchar(3) not null,
  language varchar(30) not null,
  isofficial boolean default false not null,
  percentage decimal(4,1) not null,
  constraint countrylanguage_pk primary key (countrycode, language),
  constraint countrylanguage_country_fk foreign key (countrycode) references world.country(code)
);