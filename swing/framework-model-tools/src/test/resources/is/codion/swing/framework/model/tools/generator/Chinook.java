package is.codion.chinook.domain;

import static is.codion.chinook.domain.Chinook.DOMAIN;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
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
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import java.time.LocalDate;

public final class Chinook extends DomainModel {
	public static final DomainType DOMAIN = domainType(Chinook.class);

	public Chinook() {
		super(DOMAIN);
		add(artist(), employee(), genre(),
				mediatype(), playlist(), users(),
				customer(), invoice(), album(),
				track(), invoiceline(), playlisttrack());
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

	public interface Artist {
		EntityType TYPE = DOMAIN.entityType("chinook.artist");

		Column<Long> ARTISTID = TYPE.longColumn("artistid");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	public interface Employee {
		EntityType TYPE = DOMAIN.entityType("chinook.employee");

		Column<Long> EMPLOYEEID = TYPE.longColumn("employeeid");
		Column<String> LASTNAME = TYPE.stringColumn("lastname");
		Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
		Column<String> TITLE = TYPE.stringColumn("title");
		Column<Long> REPORTSTO = TYPE.longColumn("reportsto");
		Column<LocalDate> BIRTHDATE = TYPE.localDateColumn("birthdate");
		Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
		Column<String> ADDRESS = TYPE.stringColumn("address");
		Column<String> CITY = TYPE.stringColumn("city");
		Column<String> STATE = TYPE.stringColumn("state");
		Column<String> COUNTRY = TYPE.stringColumn("country");
		Column<String> POSTALCODE = TYPE.stringColumn("postalcode");
		Column<String> PHONE = TYPE.stringColumn("phone");
		Column<String> FAX = TYPE.stringColumn("fax");
		Column<String> EMAIL = TYPE.stringColumn("email");

		ForeignKey REPORTSTO_FK = TYPE.foreignKey("reportsto_fk", REPORTSTO, Employee.EMPLOYEEID);
	}

	public interface Genre {
		EntityType TYPE = DOMAIN.entityType("chinook.genre");

		Column<Long> GENREID = TYPE.longColumn("genreid");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	public interface Mediatype {
		EntityType TYPE = DOMAIN.entityType("chinook.mediatype");

		Column<Long> MEDIATYPEID = TYPE.longColumn("mediatypeid");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	public interface Playlist {
		EntityType TYPE = DOMAIN.entityType("chinook.playlist");

		Column<Long> PLAYLISTID = TYPE.longColumn("playlistid");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	public interface Users {
		EntityType TYPE = DOMAIN.entityType("chinook.users");

		Column<Long> USERID = TYPE.longColumn("userid");
		Column<String> USERNAME = TYPE.stringColumn("username");
		Column<Integer> PASSWORDHASH = TYPE.integerColumn("passwordhash");
	}

	public interface Customer {
		EntityType TYPE = DOMAIN.entityType("chinook.customer");

		Column<Long> CUSTOMERID = TYPE.longColumn("customerid");
		Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
		Column<String> LASTNAME = TYPE.stringColumn("lastname");
		Column<String> COMPANY = TYPE.stringColumn("company");
		Column<String> ADDRESS = TYPE.stringColumn("address");
		Column<String> CITY = TYPE.stringColumn("city");
		Column<String> STATE = TYPE.stringColumn("state");
		Column<String> COUNTRY = TYPE.stringColumn("country");
		Column<String> POSTALCODE = TYPE.stringColumn("postalcode");
		Column<String> PHONE = TYPE.stringColumn("phone");
		Column<String> FAX = TYPE.stringColumn("fax");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<Long> SUPPORTREPID = TYPE.longColumn("supportrepid");

		ForeignKey SUPPORTREPID_FK = TYPE.foreignKey("supportrepid_fk", SUPPORTREPID, Employee.EMPLOYEEID);
	}

	public interface Invoice {
		EntityType TYPE = DOMAIN.entityType("chinook.invoice");

		Column<Long> INVOICEID = TYPE.longColumn("invoiceid");
		Column<Long> CUSTOMERID = TYPE.longColumn("customerid");
		Column<LocalDate> INVOICEDATE = TYPE.localDateColumn("invoicedate");
		Column<String> BILLINGADDRESS = TYPE.stringColumn("billingaddress");
		Column<String> BILLINGCITY = TYPE.stringColumn("billingcity");
		Column<String> BILLINGSTATE = TYPE.stringColumn("billingstate");
		Column<String> BILLINGCOUNTRY = TYPE.stringColumn("billingcountry");
		Column<String> BILLINGPOSTALCODE = TYPE.stringColumn("billingpostalcode");
		Column<Double> TOTAL = TYPE.doubleColumn("total");

		ForeignKey CUSTOMERID_FK = TYPE.foreignKey("customerid_fk", CUSTOMERID, Customer.CUSTOMERID);
	}

	public interface Album {
		EntityType TYPE = DOMAIN.entityType("chinook.album");

		Column<Long> ALBUMID = TYPE.longColumn("albumid");
		Column<String> TITLE = TYPE.stringColumn("title");
		Column<Long> ARTISTID = TYPE.longColumn("artistid");
		Column<byte[]> COVER = TYPE.byteArrayColumn("cover");
		Column<Object> TAGS = TYPE.column("tags", Object.class);

		ForeignKey ARTISTID_FK = TYPE.foreignKey("artistid_fk", ARTISTID, Artist.ARTISTID);
	}

	public interface Track {
		EntityType TYPE = DOMAIN.entityType("chinook.track");

		Column<Long> TRACKID = TYPE.longColumn("trackid");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Long> ALBUMID = TYPE.longColumn("albumid");
		Column<Long> MEDIATYPEID = TYPE.longColumn("mediatypeid");
		Column<Long> GENREID = TYPE.longColumn("genreid");
		Column<String> COMPOSER = TYPE.stringColumn("composer");
		Column<Integer> MILLISECONDS = TYPE.integerColumn("milliseconds");
		Column<Integer> BYTES = TYPE.integerColumn("bytes");
		Column<Integer> RATING = TYPE.integerColumn("rating");
		Column<Integer> UNITPRICE = TYPE.integerColumn("unitprice");

		ForeignKey ALBUMID_FK = TYPE.foreignKey("albumid_fk", ALBUMID, Album.ALBUMID);
		ForeignKey MEDIATYPEID_FK = TYPE.foreignKey("mediatypeid_fk", MEDIATYPEID, Mediatype.MEDIATYPEID);
		ForeignKey GENREID_FK = TYPE.foreignKey("genreid_fk", GENREID, Genre.GENREID);
	}

	public interface Invoiceline {
		EntityType TYPE = DOMAIN.entityType("chinook.invoiceline");

		Column<Long> INVOICELINEID = TYPE.longColumn("invoicelineid");
		Column<Long> INVOICEID = TYPE.longColumn("invoiceid");
		Column<Long> TRACKID = TYPE.longColumn("trackid");
		Column<Integer> UNITPRICE = TYPE.integerColumn("unitprice");
		Column<Integer> QUANTITY = TYPE.integerColumn("quantity");

		ForeignKey INVOICEID_FK = TYPE.foreignKey("invoiceid_fk", INVOICEID, Invoice.INVOICEID);
		ForeignKey TRACKID_FK = TYPE.foreignKey("trackid_fk", TRACKID, Track.TRACKID);
	}

	public interface Playlisttrack {
		EntityType TYPE = DOMAIN.entityType("chinook.playlisttrack");

		Column<Long> PLAYLISTTRACKID = TYPE.longColumn("playlisttrackid");
		Column<Long> PLAYLISTID = TYPE.longColumn("playlistid");
		Column<Long> TRACKID = TYPE.longColumn("trackid");

		ForeignKey PLAYLISTID_FK = TYPE.foreignKey("playlistid_fk", PLAYLISTID, Playlist.PLAYLISTID);
		ForeignKey TRACKID_FK = TYPE.foreignKey("trackid_fk", TRACKID, Track.TRACKID);
	}
}