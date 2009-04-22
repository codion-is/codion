create schema petstore;

create table petstore.category(
    categoryid INTEGER NOT NULL,
    name VARCHAR(25) NOT NULL,
    description VARCHAR(255) NOT NULL,
    imageurl VARCHAR(55),
    primary key (categoryid)
);

CREATE TABLE petstore.product (
 productid INTEGER NOT NULL,
 categoryid INTEGER NOT NULL,
 name VARCHAR(25) NOT NULL,
 description VARCHAR(255) NOT NULL,
 imageurl VARCHAR(55),
 primary key (productid),
 foreign key (categoryid) references petstore.category(categoryid)
);

CREATE TABLE petstore.Address (
 addressid INTEGER NOT NULL,
 street1 VARCHAR(55) NOT NULL,
 street2 VARCHAR(55),
 city VARCHAR(55) NOT NULL,
 state VARCHAR(25) NOT NULL,
 zip INTEGER NOT NULL,
 latitude DECIMAL(14,10) NOT NULL,
 longitude DECIMAL(14,10) NOT NULL,
 primary key (addressid)
);

CREATE TABLE petstore.SellerContactInfo (
 contactinfoid INTEGER NOT NULL,
 lastname VARCHAR(24) NOT NULL,
 firstname VARCHAR(24) NOT NULL,
 email VARCHAR(24) NOT NULL,
 primary key (contactinfoid)
);

CREATE TABLE petstore.item (
 itemid INTEGER NOT NULL,
 productid INTEGER NOT NULL,
 name VARCHAR(30) NOT NULL,
 description VARCHAR(500) NOT NULL,
 imageurl VARCHAR(55),
 imagethumburl VARCHAR(55),
 price DECIMAL(14,2) NOT NULL,
 address_addressid INTEGER NOT NULL,
 contactinfo_contactinfoid INTEGER NOT NULL,
 totalscore INTEGER,
 numberofvotes INTEGER,
 disabled INTEGER NOT NULL default 0,
 primary key (itemid),
 foreign key (address_addressid) references petstore.Address(addressid),
 foreign key (productid) references petstore.product(productid),
 foreign key (contactinfo_contactinfoid) references petstore.SellerContactInfo(contactinfoid)
);

create table petstore.tag(
    tagid INTEGER NOT NULL,
    tag VARCHAR(30) NOT NULL,
    primary key (tagid),
    unique(tag)
);

create table petstore.tag_item(
    tagid INTEGER NOT NULL,
    itemid INTEGER NOT NULL,
    unique(tagid, itemid),
    foreign key (itemid) references petstore.item(itemid),
    foreign key (tagid) references petstore.tag(tagid)
);
