package is.codion.chinook.domain;

import static is.codion.chinook.domain.api.Chinook.Album;
import static is.codion.chinook.domain.api.Chinook.Artist;
import static is.codion.chinook.domain.api.Chinook.Customer;
import static is.codion.chinook.domain.api.Chinook.DOMAIN;
import static is.codion.chinook.domain.api.Chinook.Employee;
import static is.codion.chinook.domain.api.Chinook.Genre;
import static is.codion.chinook.domain.api.Chinook.Invoice;
import static is.codion.chinook.domain.api.Chinook.Invoiceline;
import static is.codion.chinook.domain.api.Chinook.Mediatype;
import static is.codion.chinook.domain.api.Chinook.Playlist;
import static is.codion.chinook.domain.api.Chinook.Playlisttrack;
import static is.codion.chinook.domain.api.Chinook.Track;
import static is.codion.chinook.domain.api.Chinook.Users;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;

public final class ChinookImpl extends DomainModel {
	public ChinookImpl() {
		super(DOMAIN);
		add(artist(), employee(), genre(),
				mediatype(), playlist(), users(),
				album(), customer(), invoice(),
				track(), invoiceline(), playlisttrack());
	}

	static EntityDefinition artist() {
		return Artist.TYPE.define(
				Artist.ARTISTID.define()
					.primaryKey()
					.generator(identity()),
				Artist.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Artist")
			.build();
	}

	static EntityDefinition employee() {
		return Employee.TYPE.define(
				Employee.EMPLOYEEID.define()
					.primaryKey()
					.generator(identity()),
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
			.caption("Employee")
			.build();
	}

	static EntityDefinition genre() {
		return Genre.TYPE.define(
				Genre.GENREID.define()
					.primaryKey()
					.generator(identity()),
				Genre.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Genre")
			.build();
	}

	static EntityDefinition mediatype() {
		return Mediatype.TYPE.define(
				Mediatype.MEDIATYPEID.define()
					.primaryKey()
					.generator(identity()),
				Mediatype.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Mediatype")
			.build();
	}

	static EntityDefinition playlist() {
		return Playlist.TYPE.define(
				Playlist.PLAYLISTID.define()
					.primaryKey()
					.generator(identity()),
				Playlist.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Playlist")
			.build();
	}

	static EntityDefinition users() {
		return Users.TYPE.define(
				Users.USERID.define()
					.primaryKey()
					.generator(identity()),
				Users.USERNAME.define()
					.column()
					.caption("Username")
					.nullable(false)
					.maximumLength(20),
				Users.PASSWORDHASH.define()
					.column()
					.caption("Passwordhash")
					.nullable(false))
			.caption("Users")
			.build();
	}

	static EntityDefinition album() {
		return Album.TYPE.define(
				Album.ALBUMID.define()
					.primaryKey()
					.generator(identity()),
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
			.caption("Album")
			.build();
	}

	static EntityDefinition customer() {
		return Customer.TYPE.define(
				Customer.CUSTOMERID.define()
					.primaryKey()
					.generator(identity()),
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
			.caption("Customer")
			.build();
	}

	static EntityDefinition invoice() {
		return Invoice.TYPE.define(
				Invoice.INVOICEID.define()
					.primaryKey()
					.generator(identity()),
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
					.fractionDigits(2))
			.caption("Invoice")
			.build();
	}

	static EntityDefinition track() {
		return Track.TYPE.define(
				Track.TRACKID.define()
					.primaryKey()
					.generator(identity()),
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
			.caption("Track")
			.build();
	}

	static EntityDefinition invoiceline() {
		return Invoiceline.TYPE.define(
				Invoiceline.INVOICELINEID.define()
					.primaryKey()
					.generator(identity()),
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
			.caption("Invoiceline")
			.build();
	}

	static EntityDefinition playlisttrack() {
		return Playlisttrack.TYPE.define(
				Playlisttrack.PLAYLISTTRACKID.define()
					.primaryKey()
					.generator(identity()),
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
			.caption("Playlisttrack")
			.build();
	}
}