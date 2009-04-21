create schema petstore;

create table petstore.category(
    categoryid VARCHAR(10) NOT NULL,
    name VARCHAR(25) NOT NULL,
    description VARCHAR(255) NOT NULL,
    imageurl VARCHAR(55),
    primary key (categoryid)
);

CREATE TABLE petstore.product (
 productid VARCHAR(10) NOT NULL,
 categoryid VARCHAR(10) NOT NULL,
 name VARCHAR(25) NOT NULL,
 description VARCHAR(255) NOT NULL,
 imageurl VARCHAR(55),
 primary key (productid),
 foreign key (categoryid) references petstore.category(categoryid)
);

CREATE TABLE petstore.Address (
 addressid VARCHAR(10) NOT NULL,
 street1 VARCHAR(55) NOT NULL,
 street2 VARCHAR(55),
 city VARCHAR(55) NOT NULL,
 state VARCHAR(25) NOT NULL,
 zip VARCHAR(5) NOT NULL,
 latitude DECIMAL(14,10) NOT NULL,
 longitude DECIMAL(14,10) NOT NULL,
 primary key (addressid)
);

CREATE TABLE petstore.SellerContactInfo (
 contactinfoid VARCHAR(10) NOT NULL,
 lastname VARCHAR(24) NOT NULL,
 firstname VARCHAR(24) NOT NULL,
 email VARCHAR(24) NOT NULL,
 primary key (contactinfoid)
);

CREATE TABLE petstore.item (
 itemid VARCHAR(10) NOT NULL,
 productid VARCHAR(10) NOT NULL,
 name VARCHAR(30) NOT NULL,
 description VARCHAR(500) NOT NULL,
 imageurl VARCHAR(55),
 imagethumburl VARCHAR(55),
 price DECIMAL(14,2) NOT NULL,
 address_addressid VARCHAR(10) NOT NULL,
 contactinfo_contactinfoid VARCHAR(10) NOT NULL,
 totalscore INTEGER NOT NULL,
 numberofvotes INTEGER NOT NULL,
 disabled INTEGER NOT NULL,
 primary key (itemid),
 foreign key (address_addressid) references petstore.Address(addressid),
 foreign key (productid) references petstore.product(productid),
 foreign key (contactinfo_contactinfoid) references petstore.SellerContactInfo(contactinfoid)
);

CREATE TABLE petstore.id_gen (
 gen_key VARCHAR(20) NOT NULL,
 gen_value INTEGER NOT NULL,
 primary key (gen_key)
);

CREATE TABLE petstore.ziplocation (
 zipcode INTEGER NOT NULL,
 city VARCHAR(30) NOT NULL,
 state VARCHAR(2) NOT NULL,
 primary key (zipcode)
);

create table petstore.tag(
    tagid INTEGER NOT NULL,
    tag VARCHAR(30) NOT NULL,
    refcount INTEGER NOT NULL,
    primary key (tagid),
    unique(tag)
);

create table petstore.tag_item(
    tagid INTEGER NOT NULL,
    itemid VARCHAR(10) NOT NULL,
    unique(tagid, itemid),
    foreign key (itemid) references petstore.item(itemid),
    foreign key (tagid) references petstore.tag(tagid)
);
