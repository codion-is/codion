package is.codion.chinook.domain;

import static is.codion.chinook.domain.Chinook.Album;
import static is.codion.chinook.domain.Chinook.Artist;
import static is.codion.chinook.domain.Chinook.Customer;
import static is.codion.chinook.domain.Chinook.DOMAIN;
import static is.codion.chinook.domain.Chinook.Employee;
import static is.codion.chinook.domain.Chinook.Genre;
import static is.codion.chinook.domain.Chinook.Invoice;
import static is.codion.chinook.domain.Chinook.Invoiceline;
import static is.codion.chinook.domain.Chinook.Mediatype;
import static is.codion.chinook.domain.Chinook.Playlist;
import static is.codion.chinook.domain.Chinook.Playlisttrack;
import static is.codion.chinook.domain.Chinook.Track;
import static is.codion.chinook.domain.Chinook.Users;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
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

	public interface Artist {
		EntityType TYPE = DOMAIN.entityType("chinook.artist");

		Column<Long> ARTISTID = TYPE.longColumn("artistid");
		Column<String> NAME = TYPE.stringColumn("name");

		static Dto dto(Entity artist) {
			return artist == null ? null :
				new Dto(artist.get(ARTISTID),
					artist.get(NAME));
		}

		record Dto(Long artistid, String name) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(ARTISTID, artistid)
					.with(NAME, name)
					.build();
			}
		}
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

		static Dto dto(Entity employee) {
			return employee == null ? null :
				new Dto(employee.get(EMPLOYEEID),
					employee.get(LASTNAME),
					employee.get(FIRSTNAME),
					employee.get(TITLE),
					Employee.dto(employee.get(REPORTSTO_FK)),
					employee.get(BIRTHDATE),
					employee.get(HIREDATE),
					employee.get(ADDRESS),
					employee.get(CITY),
					employee.get(STATE),
					employee.get(COUNTRY),
					employee.get(POSTALCODE),
					employee.get(PHONE),
					employee.get(FAX),
					employee.get(EMAIL));
		}

		record Dto(Long employeeid, String lastname, String firstname, String title,
				Employee.Dto reportsto, LocalDate birthdate, LocalDate hiredate, String address, String city,
				String state, String country, String postalcode, String phone, String fax, String email) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(EMPLOYEEID, employeeid)
					.with(LASTNAME, lastname)
					.with(FIRSTNAME, firstname)
					.with(TITLE, title)
					.with(REPORTSTO_FK, reportsto.entity(entities))
					.with(BIRTHDATE, birthdate)
					.with(HIREDATE, hiredate)
					.with(ADDRESS, address)
					.with(CITY, city)
					.with(STATE, state)
					.with(COUNTRY, country)
					.with(POSTALCODE, postalcode)
					.with(PHONE, phone)
					.with(FAX, fax)
					.with(EMAIL, email)
					.build();
			}
		}
	}

	public interface Genre {
		EntityType TYPE = DOMAIN.entityType("chinook.genre");

		Column<Long> GENREID = TYPE.longColumn("genreid");
		Column<String> NAME = TYPE.stringColumn("name");

		static Dto dto(Entity genre) {
			return genre == null ? null :
				new Dto(genre.get(GENREID),
					genre.get(NAME));
		}

		record Dto(Long genreid, String name) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(GENREID, genreid)
					.with(NAME, name)
					.build();
			}
		}
	}

	public interface Mediatype {
		EntityType TYPE = DOMAIN.entityType("chinook.mediatype");

		Column<Long> MEDIATYPEID = TYPE.longColumn("mediatypeid");
		Column<String> NAME = TYPE.stringColumn("name");

		static Dto dto(Entity mediatype) {
			return mediatype == null ? null :
				new Dto(mediatype.get(MEDIATYPEID),
					mediatype.get(NAME));
		}

		record Dto(Long mediatypeid, String name) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(MEDIATYPEID, mediatypeid)
					.with(NAME, name)
					.build();
			}
		}
	}

	public interface Playlist {
		EntityType TYPE = DOMAIN.entityType("chinook.playlist");

		Column<Long> PLAYLISTID = TYPE.longColumn("playlistid");
		Column<String> NAME = TYPE.stringColumn("name");

		static Dto dto(Entity playlist) {
			return playlist == null ? null :
				new Dto(playlist.get(PLAYLISTID),
					playlist.get(NAME));
		}

		record Dto(Long playlistid, String name) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(PLAYLISTID, playlistid)
					.with(NAME, name)
					.build();
			}
		}
	}

	public interface Users {
		EntityType TYPE = DOMAIN.entityType("chinook.users");

		Column<Long> USERID = TYPE.longColumn("userid");
		Column<String> USERNAME = TYPE.stringColumn("username");
		Column<Integer> PASSWORDHASH = TYPE.integerColumn("passwordhash");

		static Dto dto(Entity users) {
			return users == null ? null :
				new Dto(users.get(USERID),
					users.get(USERNAME),
					users.get(PASSWORDHASH));
		}

		record Dto(Long userid, String username, Integer passwordhash) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(USERID, userid)
					.with(USERNAME, username)
					.with(PASSWORDHASH, passwordhash)
					.build();
			}
		}
	}

	public interface Album {
		EntityType TYPE = DOMAIN.entityType("chinook.album");

		Column<Long> ALBUMID = TYPE.longColumn("albumid");
		Column<String> TITLE = TYPE.stringColumn("title");
		Column<Long> ARTISTID = TYPE.longColumn("artistid");
		Column<byte[]> COVER = TYPE.byteArrayColumn("cover");
		Column<Object> TAGS = TYPE.column("tags", Object.class);

		ForeignKey ARTISTID_FK = TYPE.foreignKey("artistid_fk", ARTISTID, Artist.ARTISTID);

		static Dto dto(Entity album) {
			return album == null ? null :
				new Dto(album.get(ALBUMID),
					album.get(TITLE),
					Artist.dto(album.get(ARTISTID_FK)),
					album.get(COVER),
					album.get(TAGS));
		}

		record Dto(Long albumid, String title, Artist.Dto artistid, byte[] cover, Object tags) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(ALBUMID, albumid)
					.with(TITLE, title)
					.with(ARTISTID_FK, artistid.entity(entities))
					.with(COVER, cover)
					.with(TAGS, tags)
					.build();
			}
		}
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

		static Dto dto(Entity customer) {
			return customer == null ? null :
				new Dto(customer.get(CUSTOMERID),
					customer.get(FIRSTNAME),
					customer.get(LASTNAME),
					customer.get(COMPANY),
					customer.get(ADDRESS),
					customer.get(CITY),
					customer.get(STATE),
					customer.get(COUNTRY),
					customer.get(POSTALCODE),
					customer.get(PHONE),
					customer.get(FAX),
					customer.get(EMAIL),
					Employee.dto(customer.get(SUPPORTREPID_FK)));
		}

		record Dto(Long customerid, String firstname, String lastname, String company, String address,
				String city, String state, String country, String postalcode, String phone, String fax,
				String email, Employee.Dto supportrepid) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(CUSTOMERID, customerid)
					.with(FIRSTNAME, firstname)
					.with(LASTNAME, lastname)
					.with(COMPANY, company)
					.with(ADDRESS, address)
					.with(CITY, city)
					.with(STATE, state)
					.with(COUNTRY, country)
					.with(POSTALCODE, postalcode)
					.with(PHONE, phone)
					.with(FAX, fax)
					.with(EMAIL, email)
					.with(SUPPORTREPID_FK, supportrepid.entity(entities))
					.build();
			}
		}
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

		static Dto dto(Entity invoice) {
			return invoice == null ? null :
				new Dto(invoice.get(INVOICEID),
					Customer.dto(invoice.get(CUSTOMERID_FK)),
					invoice.get(INVOICEDATE),
					invoice.get(BILLINGADDRESS),
					invoice.get(BILLINGCITY),
					invoice.get(BILLINGSTATE),
					invoice.get(BILLINGCOUNTRY),
					invoice.get(BILLINGPOSTALCODE),
					invoice.get(TOTAL));
		}

		record Dto(Long invoiceid, Customer.Dto customerid, LocalDate invoicedate, String billingaddress,
				String billingcity, String billingstate, String billingcountry, String billingpostalcode,
				Double total) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(INVOICEID, invoiceid)
					.with(CUSTOMERID_FK, customerid.entity(entities))
					.with(INVOICEDATE, invoicedate)
					.with(BILLINGADDRESS, billingaddress)
					.with(BILLINGCITY, billingcity)
					.with(BILLINGSTATE, billingstate)
					.with(BILLINGCOUNTRY, billingcountry)
					.with(BILLINGPOSTALCODE, billingpostalcode)
					.with(TOTAL, total)
					.build();
			}
		}
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

		static Dto dto(Entity track) {
			return track == null ? null :
				new Dto(track.get(TRACKID),
					track.get(NAME),
					Album.dto(track.get(ALBUMID_FK)),
					Mediatype.dto(track.get(MEDIATYPEID_FK)),
					Genre.dto(track.get(GENREID_FK)),
					track.get(COMPOSER),
					track.get(MILLISECONDS),
					track.get(BYTES),
					track.get(RATING),
					track.get(UNITPRICE));
		}

		record Dto(Long trackid, String name, Album.Dto albumid, Mediatype.Dto mediatypeid,
				Genre.Dto genreid, String composer, Integer milliseconds, Integer bytes, Integer rating,
				Integer unitprice) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(TRACKID, trackid)
					.with(NAME, name)
					.with(ALBUMID_FK, albumid.entity(entities))
					.with(MEDIATYPEID_FK, mediatypeid.entity(entities))
					.with(GENREID_FK, genreid.entity(entities))
					.with(COMPOSER, composer)
					.with(MILLISECONDS, milliseconds)
					.with(BYTES, bytes)
					.with(RATING, rating)
					.with(UNITPRICE, unitprice)
					.build();
			}
		}
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

		static Dto dto(Entity invoiceline) {
			return invoiceline == null ? null :
				new Dto(invoiceline.get(INVOICELINEID),
					Invoice.dto(invoiceline.get(INVOICEID_FK)),
					Track.dto(invoiceline.get(TRACKID_FK)),
					invoiceline.get(UNITPRICE),
					invoiceline.get(QUANTITY));
		}

		record Dto(Long invoicelineid, Invoice.Dto invoiceid, Track.Dto trackid, Integer unitprice,
				Integer quantity) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(INVOICELINEID, invoicelineid)
					.with(INVOICEID_FK, invoiceid.entity(entities))
					.with(TRACKID_FK, trackid.entity(entities))
					.with(UNITPRICE, unitprice)
					.with(QUANTITY, quantity)
					.build();
			}
		}
	}

	public interface Playlisttrack {
		EntityType TYPE = DOMAIN.entityType("chinook.playlisttrack");

		Column<Long> PLAYLISTTRACKID = TYPE.longColumn("playlisttrackid");
		Column<Long> PLAYLISTID = TYPE.longColumn("playlistid");
		Column<Long> TRACKID = TYPE.longColumn("trackid");

		ForeignKey PLAYLISTID_FK = TYPE.foreignKey("playlistid_fk", PLAYLISTID, Playlist.PLAYLISTID);
		ForeignKey TRACKID_FK = TYPE.foreignKey("trackid_fk", TRACKID, Track.TRACKID);

		static Dto dto(Entity playlisttrack) {
			return playlisttrack == null ? null :
				new Dto(playlisttrack.get(PLAYLISTTRACKID),
					Playlist.dto(playlisttrack.get(PLAYLISTID_FK)),
					Track.dto(playlisttrack.get(TRACKID_FK)));
		}

		record Dto(Long playlisttrackid, Playlist.Dto playlistid, Track.Dto trackid) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(PLAYLISTTRACKID, playlisttrackid)
					.with(PLAYLISTID_FK, playlistid.entity(entities))
					.with(TRACKID_FK, trackid.entity(entities))
					.build();
			}
		}
	}
}