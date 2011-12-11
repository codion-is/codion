create schema chinook;

create table chinook.genre
(
    genreid identity not null,
    name varchar(120),
    constraint pk_genre primary key (genreid)
);

create table chinook.mediatype
(
    mediatypeid identity not null,
    name varchar(120),
    constraint pk_mediatype primary key (mediatypeid)
);

create table chinook.artist
(
    artistid identity not null,
    name varchar(120),
    constraint pk_artist primary key (artistid)
);

create table chinook.album
(
    albumid identity not null,
    title varchar(160) not null,
    artistid integer not null,
    constraint pk_productitem primary key (albumid)
);

create table chinook.track
(
    trackid identity not null,
    name varchar(200) not null,
    albumid integer,
    mediatypeid integer not null,
    genreid integer,
    composer varchar(220),
    milliseconds integer not null,
    bytes double,
    unitprice double not null,
    constraint pk_track primary key (trackid)
);

create table chinook.employee
(
    employeeid identity not null,
    lastname varchar(20) not null,
    firstname varchar(20) not null,
    title varchar(30),
    reportsto integer,
    birthdate date,
    hiredate date,
    address varchar(70),
    city varchar(40),
    state varchar(40),
    country varchar(40),
    postalcode varchar(10),
    phone varchar(24),
    fax varchar(24),
    email varchar(60),
    constraint pk_employee primary key (employeeid)
);

create table chinook.customer
(
    customerid identity not null,
    firstname varchar(40) not null,
    lastname varchar(20) not null,
    company varchar(80),
    address varchar(70),
    city varchar(40),
    state varchar(40),
    country varchar(40),
    postalcode varchar(10),
    phone varchar(24),
    fax varchar(24),
    email varchar(60) not null,
    supportrepid integer,
    constraint pk_customer primary key (customerid)
);

create table chinook.invoice
(
    invoiceid identity not null,
    customerid integer not null,
    invoicedate date not null,
    billingaddress varchar(70),
    billingcity varchar(40),
    billingstate varchar(40),
    billingcountry varchar(40),
    billingpostalcode varchar(10),
    total decimal(10, 2), --not null,
    constraint pk_invoice primary key (invoiceid)
);

create table chinook.invoiceline
(
    invoicelineid identity not null,
    invoiceid integer not null,
    trackid integer not null,
    unitprice double not null,
    quantity integer not null,
    constraint pk_invoiceline primary key (invoicelineid)
);

create table chinook.playlist
(
    playlistid identity not null,
    name varchar(120),
    constraint pk_playlist primary key (playlistid)
);

create table chinook.playlisttrack
(
    playlistid integer not null,
    trackid integer not null,
    constraint pk_playlisttrack primary key (playlistid, trackid)
);

alter table chinook.album add constraint fk_artist_album
foreign key (artistid) references chinook.artist(artistid);

alter table chinook.track add constraint fk_album_track
foreign key (albumid) references chinook.album(albumid);

alter table chinook.track add constraint fk_mediatype_track
foreign key (mediatypeid) references chinook.mediatype(mediatypeid);

alter table chinook.track add constraint fk_genre_track
foreign key (genreid) references chinook.genre(genreid);

alter table chinook.employee add constraint fk_employee_reportsto
foreign key (reportsto) references chinook.employee(employeeid);

alter table chinook.customer add constraint fk_employee_customer
foreign key (supportrepid) references chinook.employee(employeeid);

alter table chinook.invoice add constraint fk_customer_invoice
foreign key (customerid) references chinook.customer(customerid);

alter table chinook.invoiceline add constraint fk_productitem_invoiceline
foreign key (trackid) references chinook.track(trackid);

alter table chinook.invoiceline add constraint fk_invoice_invoiceline
foreign key (invoiceid) references chinook.invoice(invoiceid);

alter table chinook.playlisttrack add constraint fk_track_playlisttrack
foreign key (trackid) references chinook.track(trackid);

alter table chinook.playlisttrack add constraint fk_playlist_playlisttrack
foreign key (playlistid) references chinook.playlist(playlistid);
