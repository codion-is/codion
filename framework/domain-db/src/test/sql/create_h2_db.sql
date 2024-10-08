create schema petstore;

create table petstore.category (
 category_id integer generated by default as identity,
 name varchar(25) not null,
 description varchar(255) not null,
 image_url varchar(55),
 insert_user varchar(100) default user() not null,
 insert_time timestamp default current_time() not null,
 update_user varchar(100),
 update_time timestamp,
 primary key (category_id)
);

create table petstore.product (
 product_id integer generated by default as identity,
 category_id integer not null,
 name varchar(25) not null,
 description varchar(255) not null,
 image_url varchar(55),
 insert_user varchar(100) default user() not null,
 insert_time timestamp default current_time() not null,
 update_user varchar(100),
 update_time timestamp,
 primary key (product_id),
 foreign key (category_id) references petstore.category(category_id)
);

comment on table petstore.product is 'The available products';
comment on column petstore.product.name is 'The product name';

create table petstore.address (
 address_id integer generated by default as identity,
 street1 varchar(55) not null,
 street2 varchar(55),
 city varchar(55) not null,
 state varchar(25) not null,
 zip integer not null,
 latitude decimal(14,2) not null,
 longitude decimal(14,2) not null,
 location geometry,
 image blob,
 insert_user varchar(100) default user() not null,
 insert_time timestamp default current_time() not null,
 update_user varchar(100),
 update_time timestamp,
 primary key (address_id)
);

create table petstore.contact_info (
 contact_info_id integer generated by default as identity,
 last_name varchar(24) not null,
 first_name varchar(24) not null,
 email varchar(24) not null,
 insert_user varchar(100) default user() not null,
 insert_time timestamp default current_time() not null,
 update_user varchar(100),
 update_time timestamp,
 primary key (contact_info_id)
);

create table petstore.item (
 item_id integer generated by default as identity,
 product_id integer not null,
 name varchar(30) not null,
 description varchar(500) not null,
 image_url varchar(55),
 image_thumb_url varchar(55),
 price decimal(14,2) not null,
 address_id integer not null,
 contact_info_id integer not null,
 total_score integer,
 number_of_votes integer,
 disabled integer not null default 0,
 insert_user varchar(100) default user() not null,
 insert_time timestamp default current_time() not null,
 update_user varchar(100),
 update_time timestamp,
 primary key (item_id),
 foreign key (address_id) references petstore.address(address_id),
 foreign key (product_id) references petstore.product(product_id),
 foreign key (contact_info_id) references petstore.contact_info(contact_info_id)
);

create table petstore.tag (
 tag_id integer generated by default as identity,
 tag varchar(30) not null,
 insert_user varchar(100) default user() not null,
 insert_time timestamp default current_time() not null,
 update_user varchar(100),
 update_time timestamp,
 primary key (tag_id),
 unique(tag)
);

create table petstore.tag_item (
 tag_id integer not null,
 item_id integer not null,
 insert_user varchar(100) default user() not null,
 insert_time timestamp default current_time() not null,
 update_user varchar(100),
 update_time timestamp,
 primary key (tag_id, item_id),
 foreign key (item_id) references petstore.item(item_id),
 foreign key (tag_id) references petstore.tag(tag_id)
);

create view petstore.item_tags as
select item.name, tag.tag
from petstore.tag
join petstore.tag_item on tag.tag_id = tag_item.tag_id
join petstore.item on tag_item.item_id = item.item_id;

CREATE SCHEMA CHINOOK;

CREATE TABLE CHINOOK.USERS
(
  USERID LONG GENERATED BY DEFAULT AS IDENTITY,
  USERNAME VARCHAR(20) NOT NULL,
  PASSWORDHASH INTEGER NOT NULL,
  CONSTRAINT PK_USER PRIMARY KEY (USERID),
  CONSTRAINT UK_USER UNIQUE (USERNAME)
);

CREATE TABLE CHINOOK.GENRE
(
    GENREID LONG GENERATED BY DEFAULT AS IDENTITY,
    NAME VARCHAR(120) NOT NULL,
    CONSTRAINT PK_GENRE PRIMARY KEY (GENREID),
    CONSTRAINT UK_GENRE UNIQUE (NAME)
);

CREATE TABLE CHINOOK.MEDIATYPE
(
    MEDIATYPEID LONG GENERATED BY DEFAULT AS IDENTITY,
    NAME VARCHAR(120) NOT NULL,
    CONSTRAINT PK_MEDIATYPE PRIMARY KEY (MEDIATYPEID),
    CONSTRAINT UK_MEDIATYPE UNIQUE (NAME)
);

CREATE TABLE CHINOOK.ARTIST
(
    ARTISTID LONG GENERATED BY DEFAULT AS IDENTITY,
    NAME VARCHAR(120) NOT NULL,
    CONSTRAINT PK_ARTIST PRIMARY KEY (ARTISTID),
    CONSTRAINT UK_ARTIST UNIQUE (NAME)
);

CREATE TABLE CHINOOK.ALBUM
(
    ALBUMID LONG GENERATED BY DEFAULT AS IDENTITY,
    TITLE VARCHAR(160) NOT NULL,
    ARTISTID LONG NOT NULL,
    COVER BLOB,
    TAGS VARCHAR ARRAY,
    CONSTRAINT PK_ALBUM PRIMARY KEY (ALBUMID),
    CONSTRAINT FK_ARTIST_ALBUM FOREIGN KEY (ARTISTID) REFERENCES CHINOOK.ARTIST(ARTISTID)
);

CREATE TABLE CHINOOK.TRACK
(
    TRACKID LONG GENERATED BY DEFAULT AS IDENTITY,
    NAME VARCHAR(200) NOT NULL,
    ALBUMID LONG NOT NULL,
    MEDIATYPEID LONG NOT NULL,
    GENREID LONG,
    COMPOSER VARCHAR(220),
    MILLISECONDS INTEGER NOT NULL,
    BYTES DOUBLE,
    RATING INTEGER NOT NULL,
    UNITPRICE DOUBLE NOT NULL,
    CONSTRAINT PK_TRACK PRIMARY KEY (TRACKID),
    CONSTRAINT FK_ALBUM_TRACK FOREIGN KEY (ALBUMID) REFERENCES CHINOOK.ALBUM(ALBUMID),
    CONSTRAINT FK_MEDIATYPE_TRACK FOREIGN KEY (MEDIATYPEID) REFERENCES CHINOOK.MEDIATYPE(MEDIATYPEID),
    CONSTRAINT FK_GENRE_TRACK FOREIGN KEY (GENREID) REFERENCES CHINOOK.GENRE(GENREID),
    CONSTRAINT CHK_RATING CHECK (RATING BETWEEN 1 AND 10)
);

CREATE TABLE CHINOOK.EMPLOYEE
(
    EMPLOYEEID LONG GENERATED BY DEFAULT AS IDENTITY,
    LASTNAME VARCHAR(20) NOT NULL,
    FIRSTNAME VARCHAR(20) NOT NULL,
    TITLE VARCHAR(30),
    REPORTSTO LONG,
    BIRTHDATE DATE,
    HIREDATE DATE,
    ADDRESS VARCHAR(70),
    CITY VARCHAR(40),
    STATE VARCHAR(40),
    COUNTRY VARCHAR(40),
    POSTALCODE VARCHAR(10),
    PHONE VARCHAR(24),
    FAX VARCHAR(24),
    EMAIL VARCHAR(60) NOT NULL,
    CONSTRAINT PK_EMPLOYEE PRIMARY KEY (EMPLOYEEID),
    CONSTRAINT FK_EMPLOYEE_REPORTSTO FOREIGN KEY (REPORTSTO) REFERENCES CHINOOK.EMPLOYEE(EMPLOYEEID)
);

CREATE TABLE CHINOOK.CUSTOMER
(
    CUSTOMERID LONG GENERATED BY DEFAULT AS IDENTITY,
    FIRSTNAME VARCHAR(40) NOT NULL,
    LASTNAME VARCHAR(20) NOT NULL,
    COMPANY VARCHAR(80),
    ADDRESS VARCHAR(70),
    CITY VARCHAR(40),
    STATE VARCHAR(40),
    COUNTRY VARCHAR(40),
    POSTALCODE VARCHAR(10),
    PHONE VARCHAR(24),
    FAX VARCHAR(24),
    EMAIL VARCHAR(60) NOT NULL,
    SUPPORTREPID LONG,
    CONSTRAINT PK_CUSTOMER PRIMARY KEY (CUSTOMERID),
    CONSTRAINT FK_EMPLOYEE_CUSTOMER FOREIGN KEY (SUPPORTREPID) REFERENCES CHINOOK.EMPLOYEE(EMPLOYEEID)
);

CREATE TABLE CHINOOK.INVOICE
(
    INVOICEID LONG GENERATED BY DEFAULT AS IDENTITY,
    CUSTOMERID LONG NOT NULL,
    INVOICEDATE DATE NOT NULL,
    BILLINGADDRESS VARCHAR(70),
    BILLINGCITY VARCHAR(40),
    BILLINGSTATE VARCHAR(40),
    BILLINGCOUNTRY VARCHAR(40),
    BILLINGPOSTALCODE VARCHAR(10),
    TOTAL DECIMAL(10, 2),
    CONSTRAINT PK_INVOICE PRIMARY KEY (INVOICEID),
    CONSTRAINT FK_CUSTOMER_INVOICE FOREIGN KEY (CUSTOMERID) REFERENCES CHINOOK.CUSTOMER(CUSTOMERID)
);

CREATE TABLE CHINOOK.INVOICELINE
(
    INVOICELINEID LONG GENERATED BY DEFAULT AS IDENTITY,
    INVOICEID LONG NOT NULL,
    TRACKID LONG NOT NULL,
    UNITPRICE DOUBLE NOT NULL,
    QUANTITY INTEGER NOT NULL,
    CONSTRAINT PK_INVOICELINE PRIMARY KEY (INVOICELINEID),
    CONSTRAINT FK_TRACK_INVOICELINE FOREIGN KEY (TRACKID) REFERENCES CHINOOK.TRACK(TRACKID),
    CONSTRAINT FK_INVOICE_INVOICELINE FOREIGN KEY (INVOICEID) REFERENCES CHINOOK.INVOICE(INVOICEID),
    CONSTRAINT UK_INVOICELINE_INVOICE_TRACK UNIQUE (INVOICEID, TRACKID)
);

CREATE TABLE CHINOOK.PLAYLIST
(
    PLAYLISTID LONG GENERATED BY DEFAULT AS IDENTITY,
    NAME VARCHAR(120) NOT NULL,
    CONSTRAINT PK_PLAYLIST PRIMARY KEY (PLAYLISTID),
    CONSTRAINT UK_PLAYLIST UNIQUE (NAME)
);

CREATE TABLE CHINOOK.PLAYLISTTRACK
(
    PLAYLISTTRACKID LONG GENERATED BY DEFAULT AS IDENTITY,
    PLAYLISTID LONG NOT NULL,
    TRACKID LONG NOT NULL,
    CONSTRAINT PK_PLAYLISTTRACK PRIMARY KEY (PLAYLISTTRACKID),
    CONSTRAINT UK_PLAYLISTTRACK UNIQUE (PLAYLISTID, TRACKID),
    CONSTRAINT FK_TRACK_PLAYLISTTRACK FOREIGN KEY (TRACKID) REFERENCES CHINOOK.TRACK(TRACKID),
    CONSTRAINT FK_PLAYLIST_PLAYLISTTRACK FOREIGN KEY (PLAYLISTID) REFERENCES CHINOOK.PLAYLIST(PLAYLISTID)
);

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
  flag blob,
  constraint country_pk primary key (code),
  constraint continent_chk check(continent in ('Asia','Europe','North America','Africa','Oceania','Antarctica','South America'))
);

create table world.city (
  id int not null,
  name varchar(35) not null,
  countrycode varchar(3) not null,
  district varchar(20) not null,
  population int not null,
  location geometry(point),
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

create or replace view world.country_city_v as
select country.code countrycode, country.name countryname, country.continent, country.region,
  country.surfacearea, country.indepyear, country.population countrypopulation, country.lifeexpectancy,
  country.gnp, country.gnpold, country.localname, country.governmentform, country.headofstate,
  country.capital, country.code2, country.flag,
  city.id cityid, city.name cityname, city.district, city.population citypopulation
from world.country join world.city on city.countrycode = country.code;

alter table world.city
add constraint city_country_fk
foreign key (countrycode)
references world.country(code);

alter table world.country
add constraint country_capital_fk
foreign key (capital)
references world.city(id);

create user scott password 'tiger';
alter user scott admin true;