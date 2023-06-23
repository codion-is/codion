create schema petstore;

create table petstore.category(
 categoryid integer not null,
 name varchar(25) not null,
 description varchar(255) not null,
 imageurl varchar(55),
 primary key (categoryid)
);

create table petstore.product (
 productid integer not null,
 categoryid integer not null,
 name varchar(25) not null,
 description varchar(255) not null,
 imageurl varchar(55),
 primary key (productid),
 foreign key (categoryid) references petstore.category(categoryid)
);

create table petstore.address (
 addressid integer not null,
 street1 varchar(55) not null,
 street2 varchar(55),
 city varchar(55) not null,
 state varchar(25) not null,
 zip integer not null,
 latitude decimal(14,2) not null,
 longitude decimal(14,2) not null,
 primary key (addressid)
);

create table petstore.sellercontactinfo (
 contactinfoid integer not null,
 lastname varchar(24) not null,
 firstname varchar(24) not null,
 email varchar(24) not null,
 primary key (contactinfoid)
);

create table petstore.item (
 itemid integer not null,
 productid integer not null,
 name varchar(30) not null,
 description varchar(500) not null,
 imageurl varchar(55),
 imagethumburl varchar(55),
 price decimal(14,2) not null,
 address_addressid integer not null,
 contactinfo_contactinfoid integer not null,
 totalscore integer,
 numberofvotes integer,
 disabled integer,
 primary key (itemid),
 foreign key (address_addressid) references petstore.address(addressid),
 foreign key (productid) references petstore.product(productid),
 foreign key (contactinfo_contactinfoid) references petstore.sellercontactinfo(contactinfoid)
);

create table petstore.tag(
 tagid integer not null,
 tag varchar(30) not null,
 primary key (tagid),
 unique(tag)
);

create table petstore.tag_item(
 tagid integer not null,
 itemid integer not null,
 primary key (tagid, itemid),
 foreign key (itemid) references petstore.item(itemid),
 foreign key (tagid) references petstore.tag(tagid)
);

create user scott password 'tiger';
alter user scott admin true;