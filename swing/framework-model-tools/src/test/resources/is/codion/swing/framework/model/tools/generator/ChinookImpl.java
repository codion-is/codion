package is.codion.chinook.domain.impl;

import static is.codion.chinook.domain.Chinook.DOMAIN;
import static is.codion.framework.domain.entity.KeyGenerator.identity;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.chinook.domain.Chinook.Album;
import is.codion.chinook.domain.Chinook.Artist;
import is.codion.chinook.domain.Chinook.Customer;
import is.codion.chinook.domain.Chinook.Employee;
import is.codion.chinook.domain.Chinook.Genre;
import is.codion.chinook.domain.Chinook.Invoice;
import is.codion.chinook.domain.Chinook.Invoiceline;
import is.codion.chinook.domain.Chinook.Mediatype;
import is.codion.chinook.domain.Chinook.Playlist;
import is.codion.chinook.domain.Chinook.Playlisttrack;
import is.codion.chinook.domain.Chinook.Track;
import is.codion.chinook.domain.Chinook.Users;

public final class ChinookImpl extends DomainModel {
	public ChinookImpl() {
		super(DOMAIN);
		add(album(), artist(), customer(),
				employee(), genre(), invoice(),
				invoiceline(), mediatype(), playlist(),
				playlisttrack(), track(), users());
	}

	static EntityDefinition album() {
		return Album.TYPE.define(
				Album.ALBUMID.define()
					.primaryKey(),
				Album.TITLE.define()
					.column()
					.caption("Title")
					.nullable(false)
					.maximumLength(160),
				Album.ARTISTID.define()
					.column()
					.nullable(false),
				Album.ARTISTID_FK.define()
					.foreignKey()
					.caption("Artist"),
				Album.COVER.define()
					.column()
					.caption("Cover"),
				Album.TAGS.define()
					.column()
					.caption("Tags"))
			.keyGenerator(identity())
			.caption("Album")
			.build();
	}

	static EntityDefinition artist() {
		return Artist.TYPE.define(
				Artist.ARTISTID.define()
					.primaryKey(),
				Artist.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.keyGenerator(identity())
			.caption("Artist")
			.build();
	}

	static EntityDefinition customer() {
		return Customer.TYPE.define(
				Customer.CUSTOMERID.define()
					.primaryKey(),
				Customer.FIRSTNAME.define()
					.column()
					.caption("Firstname")
					.nullable(false)
					.maximumLength(40),
				Customer.LASTNAME.define()
					.column()
					.caption("Lastname")
					.nullable(false)
					.maximumLength(20),
				Customer.COMPANY.define()
					.column()
					.caption("Company")
					.maximumLength(80),
				Customer.ADDRESS.define()
					.column()
					.caption("Address")
					.maximumLength(70),
				Customer.CITY.define()
					.column()
					.caption("City")
					.maximumLength(40),
				Customer.STATE.define()
					.column()
					.caption("State")
					.maximumLength(40),
				Customer.COUNTRY.define()
					.column()
					.caption("Country")
					.maximumLength(40),
				Customer.POSTALCODE.define()
					.column()
					.caption("Postalcode")
					.maximumLength(10),
				Customer.PHONE.define()
					.column()
					.caption("Phone")
					.maximumLength(24),
				Customer.FAX.define()
					.column()
					.caption("Fax")
					.maximumLength(24),
				Customer.EMAIL.define()
					.column()
					.caption("Email")
					.nullable(false)
					.maximumLength(60),
				Customer.SUPPORTREPID.define()
					.column(),
				Customer.SUPPORTREPID_FK.define()
					.foreignKey()
					.caption("Employee"))
			.keyGenerator(identity())
			.caption("Customer")
			.build();
	}

	static EntityDefinition employee() {
		return Employee.TYPE.define(
				Employee.EMPLOYEEID.define()
					.primaryKey(),
				Employee.LASTNAME.define()
					.column()
					.caption("Lastname")
					.nullable(false)
					.maximumLength(20),
				Employee.FIRSTNAME.define()
					.column()
					.caption("Firstname")
					.nullable(false)
					.maximumLength(20),
				Employee.TITLE.define()
					.column()
					.caption("Title")
					.maximumLength(30),
				Employee.REPORTSTO.define()
					.column(),
				Employee.REPORTSTO_FK.define()
					.foreignKey()
					.caption("Employee"),
				Employee.BIRTHDATE.define()
					.column()
					.caption("Birthdate"),
				Employee.HIREDATE.define()
					.column()
					.caption("Hiredate"),
				Employee.ADDRESS.define()
					.column()
					.caption("Address")
					.maximumLength(70),
				Employee.CITY.define()
					.column()
					.caption("City")
					.maximumLength(40),
				Employee.STATE.define()
					.column()
					.caption("State")
					.maximumLength(40),
				Employee.COUNTRY.define()
					.column()
					.caption("Country")
					.maximumLength(40),
				Employee.POSTALCODE.define()
					.column()
					.caption("Postalcode")
					.maximumLength(10),
				Employee.PHONE.define()
					.column()
					.caption("Phone")
					.maximumLength(24),
				Employee.FAX.define()
					.column()
					.caption("Fax")
					.maximumLength(24),
				Employee.EMAIL.define()
					.column()
					.caption("Email")
					.nullable(false)
					.maximumLength(60))
			.keyGenerator(identity())
			.caption("Employee")
			.build();
	}

	static EntityDefinition genre() {
		return Genre.TYPE.define(
				Genre.GENREID.define()
					.primaryKey(),
				Genre.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.keyGenerator(identity())
			.caption("Genre")
			.build();
	}

	static EntityDefinition invoice() {
		return Invoice.TYPE.define(
				Invoice.INVOICEID.define()
					.primaryKey(),
				Invoice.CUSTOMERID.define()
					.column()
					.nullable(false),
				Invoice.CUSTOMERID_FK.define()
					.foreignKey()
					.caption("Customer"),
				Invoice.INVOICEDATE.define()
					.column()
					.caption("Invoicedate")
					.nullable(false),
				Invoice.BILLINGADDRESS.define()
					.column()
					.caption("Billingaddress")
					.maximumLength(70),
				Invoice.BILLINGCITY.define()
					.column()
					.caption("Billingcity")
					.maximumLength(40),
				Invoice.BILLINGSTATE.define()
					.column()
					.caption("Billingstate")
					.maximumLength(40),
				Invoice.BILLINGCOUNTRY.define()
					.column()
					.caption("Billingcountry")
					.maximumLength(40),
				Invoice.BILLINGPOSTALCODE.define()
					.column()
					.caption("Billingpostalcode")
					.maximumLength(10),
				Invoice.TOTAL.define()
					.column()
					.caption("Total")
					.maximumFractionDigits(2))
			.keyGenerator(identity())
			.caption("Invoice")
			.build();
	}

	static EntityDefinition invoiceline() {
		return Invoiceline.TYPE.define(
				Invoiceline.INVOICELINEID.define()
					.primaryKey(),
				Invoiceline.INVOICEID.define()
					.column()
					.nullable(false),
				Invoiceline.INVOICEID_FK.define()
					.foreignKey()
					.caption("Invoice"),
				Invoiceline.TRACKID.define()
					.column()
					.nullable(false),
				Invoiceline.TRACKID_FK.define()
					.foreignKey()
					.caption("Track"),
				Invoiceline.UNITPRICE.define()
					.column()
					.caption("Unitprice")
					.nullable(false),
				Invoiceline.QUANTITY.define()
					.column()
					.caption("Quantity")
					.nullable(false))
			.keyGenerator(identity())
			.caption("Invoiceline")
			.build();
	}

	static EntityDefinition mediatype() {
		return Mediatype.TYPE.define(
				Mediatype.MEDIATYPEID.define()
					.primaryKey(),
				Mediatype.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.keyGenerator(identity())
			.caption("Mediatype")
			.build();
	}

	static EntityDefinition playlist() {
		return Playlist.TYPE.define(
				Playlist.PLAYLISTID.define()
					.primaryKey(),
				Playlist.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.keyGenerator(identity())
			.caption("Playlist")
			.build();
	}

	static EntityDefinition playlisttrack() {
		return Playlisttrack.TYPE.define(
				Playlisttrack.PLAYLISTTRACKID.define()
					.primaryKey(),
				Playlisttrack.PLAYLISTID.define()
					.column()
					.nullable(false),
				Playlisttrack.PLAYLISTID_FK.define()
					.foreignKey()
					.caption("Playlist"),
				Playlisttrack.TRACKID.define()
					.column()
					.nullable(false),
				Playlisttrack.TRACKID_FK.define()
					.foreignKey()
					.caption("Track"))
			.keyGenerator(identity())
			.caption("Playlisttrack")
			.build();
	}

	static EntityDefinition track() {
		return Track.TYPE.define(
				Track.TRACKID.define()
					.primaryKey(),
				Track.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(200),
				Track.ALBUMID.define()
					.column()
					.nullable(false),
				Track.ALBUMID_FK.define()
					.foreignKey()
					.caption("Album"),
				Track.MEDIATYPEID.define()
					.column()
					.nullable(false),
				Track.MEDIATYPEID_FK.define()
					.foreignKey()
					.caption("Mediatype"),
				Track.GENREID.define()
					.column(),
				Track.GENREID_FK.define()
					.foreignKey()
					.caption("Genre"),
				Track.COMPOSER.define()
					.column()
					.caption("Composer")
					.maximumLength(220),
				Track.MILLISECONDS.define()
					.column()
					.caption("Milliseconds")
					.nullable(false),
				Track.BYTES.define()
					.column()
					.caption("Bytes"),
				Track.RATING.define()
					.column()
					.caption("Rating")
					.nullable(false),
				Track.UNITPRICE.define()
					.column()
					.caption("Unitprice")
					.nullable(false))
			.keyGenerator(identity())
			.caption("Track")
			.build();
	}

	static EntityDefinition users() {
		return Users.TYPE.define(
				Users.USERID.define()
					.primaryKey(),
				Users.USERNAME.define()
					.column()
					.caption("Username")
					.nullable(false)
					.maximumLength(20),
				Users.PASSWORDHASH.define()
					.column()
					.caption("Passwordhash")
					.nullable(false))
			.keyGenerator(identity())
			.caption("Users")
			.build();
	}
}