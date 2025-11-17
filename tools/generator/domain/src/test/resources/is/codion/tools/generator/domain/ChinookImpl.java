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
		return Artist.TYPE.as(
				Artist.ARTISTID.as()
					.primaryKey()
					.generator(identity()),
				Artist.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Artist")
			.build();
	}

	static EntityDefinition employee() {
		return Employee.TYPE.as(
				Employee.EMPLOYEEID.as()
					.primaryKey()
					.generator(identity()),
				Employee.LASTNAME.as()
					.column()
					.caption("Lastname")
					.nullable(false)
					.maximumLength(20),
				Employee.FIRSTNAME.as()
					.column()
					.caption("Firstname")
					.nullable(false)
					.maximumLength(20),
				Employee.TITLE.as()
					.column()
					.caption("Title")
					.maximumLength(30),
				Employee.REPORTSTO.as()
					.column(),
				Employee.REPORTSTO_FK.as()
					.foreignKey()
					.caption("Employee"),
				Employee.BIRTHDATE.as()
					.column()
					.caption("Birthdate"),
				Employee.HIREDATE.as()
					.column()
					.caption("Hiredate"),
				Employee.ADDRESS.as()
					.column()
					.caption("Address")
					.maximumLength(70),
				Employee.CITY.as()
					.column()
					.caption("City")
					.maximumLength(40),
				Employee.STATE.as()
					.column()
					.caption("State")
					.maximumLength(40),
				Employee.COUNTRY.as()
					.column()
					.caption("Country")
					.maximumLength(40),
				Employee.POSTALCODE.as()
					.column()
					.caption("Postalcode")
					.maximumLength(10),
				Employee.PHONE.as()
					.column()
					.caption("Phone")
					.maximumLength(24),
				Employee.FAX.as()
					.column()
					.caption("Fax")
					.maximumLength(24),
				Employee.EMAIL.as()
					.column()
					.caption("Email")
					.nullable(false)
					.maximumLength(60))
			.caption("Employee")
			.build();
	}

	static EntityDefinition genre() {
		return Genre.TYPE.as(
				Genre.GENREID.as()
					.primaryKey()
					.generator(identity()),
				Genre.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Genre")
			.build();
	}

	static EntityDefinition mediatype() {
		return Mediatype.TYPE.as(
				Mediatype.MEDIATYPEID.as()
					.primaryKey()
					.generator(identity()),
				Mediatype.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Mediatype")
			.build();
	}

	static EntityDefinition playlist() {
		return Playlist.TYPE.as(
				Playlist.PLAYLISTID.as()
					.primaryKey()
					.generator(identity()),
				Playlist.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(120))
			.caption("Playlist")
			.build();
	}

	static EntityDefinition users() {
		return Users.TYPE.as(
				Users.USERID.as()
					.primaryKey()
					.generator(identity()),
				Users.USERNAME.as()
					.column()
					.caption("Username")
					.nullable(false)
					.maximumLength(20),
				Users.PASSWORDHASH.as()
					.column()
					.caption("Passwordhash")
					.nullable(false))
			.caption("Users")
			.build();
	}

	static EntityDefinition album() {
		return Album.TYPE.as(
				Album.ALBUMID.as()
					.primaryKey()
					.generator(identity()),
				Album.TITLE.as()
					.column()
					.caption("Title")
					.nullable(false)
					.maximumLength(160),
				Album.ARTISTID.as()
					.column()
					.nullable(false),
				Album.ARTISTID_FK.as()
					.foreignKey()
					.caption("Artist"),
				Album.COVER.as()
					.column()
					.caption("Cover"),
				Album.TAGS.as()
					.column()
					.caption("Tags"))
			.caption("Album")
			.build();
	}

	static EntityDefinition customer() {
		return Customer.TYPE.as(
				Customer.CUSTOMERID.as()
					.primaryKey()
					.generator(identity()),
				Customer.FIRSTNAME.as()
					.column()
					.caption("Firstname")
					.nullable(false)
					.maximumLength(40),
				Customer.LASTNAME.as()
					.column()
					.caption("Lastname")
					.nullable(false)
					.maximumLength(20),
				Customer.COMPANY.as()
					.column()
					.caption("Company")
					.maximumLength(80),
				Customer.ADDRESS.as()
					.column()
					.caption("Address")
					.maximumLength(70),
				Customer.CITY.as()
					.column()
					.caption("City")
					.maximumLength(40),
				Customer.STATE.as()
					.column()
					.caption("State")
					.maximumLength(40),
				Customer.COUNTRY.as()
					.column()
					.caption("Country")
					.maximumLength(40),
				Customer.POSTALCODE.as()
					.column()
					.caption("Postalcode")
					.maximumLength(10),
				Customer.PHONE.as()
					.column()
					.caption("Phone")
					.maximumLength(24),
				Customer.FAX.as()
					.column()
					.caption("Fax")
					.maximumLength(24),
				Customer.EMAIL.as()
					.column()
					.caption("Email")
					.nullable(false)
					.maximumLength(60),
				Customer.SUPPORTREPID.as()
					.column(),
				Customer.SUPPORTREPID_FK.as()
					.foreignKey()
					.caption("Employee"))
			.caption("Customer")
			.build();
	}

	static EntityDefinition invoice() {
		return Invoice.TYPE.as(
				Invoice.INVOICEID.as()
					.primaryKey()
					.generator(identity()),
				Invoice.CUSTOMERID.as()
					.column()
					.nullable(false),
				Invoice.CUSTOMERID_FK.as()
					.foreignKey()
					.caption("Customer"),
				Invoice.INVOICEDATE.as()
					.column()
					.caption("Invoicedate")
					.nullable(false),
				Invoice.BILLINGADDRESS.as()
					.column()
					.caption("Billingaddress")
					.maximumLength(70),
				Invoice.BILLINGCITY.as()
					.column()
					.caption("Billingcity")
					.maximumLength(40),
				Invoice.BILLINGSTATE.as()
					.column()
					.caption("Billingstate")
					.maximumLength(40),
				Invoice.BILLINGCOUNTRY.as()
					.column()
					.caption("Billingcountry")
					.maximumLength(40),
				Invoice.BILLINGPOSTALCODE.as()
					.column()
					.caption("Billingpostalcode")
					.maximumLength(10),
				Invoice.TOTAL.as()
					.column()
					.caption("Total")
					.fractionDigits(2))
			.caption("Invoice")
			.build();
	}

	static EntityDefinition track() {
		return Track.TYPE.as(
				Track.TRACKID.as()
					.primaryKey()
					.generator(identity()),
				Track.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(200),
				Track.ALBUMID.as()
					.column()
					.nullable(false),
				Track.ALBUMID_FK.as()
					.foreignKey()
					.caption("Album"),
				Track.MEDIATYPEID.as()
					.column()
					.nullable(false),
				Track.MEDIATYPEID_FK.as()
					.foreignKey()
					.caption("Mediatype"),
				Track.GENREID.as()
					.column(),
				Track.GENREID_FK.as()
					.foreignKey()
					.caption("Genre"),
				Track.COMPOSER.as()
					.column()
					.caption("Composer")
					.maximumLength(220),
				Track.MILLISECONDS.as()
					.column()
					.caption("Milliseconds")
					.nullable(false),
				Track.BYTES.as()
					.column()
					.caption("Bytes"),
				Track.RATING.as()
					.column()
					.caption("Rating")
					.nullable(false),
				Track.UNITPRICE.as()
					.column()
					.caption("Unitprice")
					.nullable(false))
			.caption("Track")
			.build();
	}

	static EntityDefinition invoiceline() {
		return Invoiceline.TYPE.as(
				Invoiceline.INVOICELINEID.as()
					.primaryKey()
					.generator(identity()),
				Invoiceline.INVOICEID.as()
					.column()
					.nullable(false),
				Invoiceline.INVOICEID_FK.as()
					.foreignKey()
					.caption("Invoice"),
				Invoiceline.TRACKID.as()
					.column()
					.nullable(false),
				Invoiceline.TRACKID_FK.as()
					.foreignKey()
					.caption("Track"),
				Invoiceline.UNITPRICE.as()
					.column()
					.caption("Unitprice")
					.nullable(false),
				Invoiceline.QUANTITY.as()
					.column()
					.caption("Quantity")
					.nullable(false))
			.caption("Invoiceline")
			.build();
	}

	static EntityDefinition playlisttrack() {
		return Playlisttrack.TYPE.as(
				Playlisttrack.PLAYLISTTRACKID.as()
					.primaryKey()
					.generator(identity()),
				Playlisttrack.PLAYLISTID.as()
					.column()
					.nullable(false),
				Playlisttrack.PLAYLISTID_FK.as()
					.foreignKey()
					.caption("Playlist"),
				Playlisttrack.TRACKID.as()
					.column()
					.nullable(false),
				Playlisttrack.TRACKID_FK.as()
					.foreignKey()
					.caption("Track"))
			.caption("Playlisttrack")
			.build();
	}
}