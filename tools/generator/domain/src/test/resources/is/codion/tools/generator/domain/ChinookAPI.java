package is.codion.chinook.domain.api;

import static is.codion.framework.domain.DomainType.domainType;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import java.time.LocalDate;

public interface Chinook {
	DomainType DOMAIN = domainType(Chinook.class);

	interface Artist {
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
				return entities.builder(TYPE)
					.with(ARTISTID, artistid)
					.with(NAME, name)
					.build();
			}
		}
	}

	interface Employee {
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
				return entities.builder(TYPE)
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

	interface Genre {
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
				return entities.builder(TYPE)
					.with(GENREID, genreid)
					.with(NAME, name)
					.build();
			}
		}
	}

	interface Mediatype {
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
				return entities.builder(TYPE)
					.with(MEDIATYPEID, mediatypeid)
					.with(NAME, name)
					.build();
			}
		}
	}

	interface Playlist {
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
				return entities.builder(TYPE)
					.with(PLAYLISTID, playlistid)
					.with(NAME, name)
					.build();
			}
		}
	}

	interface Users {
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
				return entities.builder(TYPE)
					.with(USERID, userid)
					.with(USERNAME, username)
					.with(PASSWORDHASH, passwordhash)
					.build();
			}
		}
	}

	interface Album {
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
				return entities.builder(TYPE)
					.with(ALBUMID, albumid)
					.with(TITLE, title)
					.with(ARTISTID_FK, artistid.entity(entities))
					.with(COVER, cover)
					.with(TAGS, tags)
					.build();
			}
		}
	}

	interface Customer {
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
				return entities.builder(TYPE)
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

	interface Invoice {
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
				return entities.builder(TYPE)
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

	interface Track {
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
				return entities.builder(TYPE)
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

	interface Invoiceline {
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
				return entities.builder(TYPE)
					.with(INVOICELINEID, invoicelineid)
					.with(INVOICEID_FK, invoiceid.entity(entities))
					.with(TRACKID_FK, trackid.entity(entities))
					.with(UNITPRICE, unitprice)
					.with(QUANTITY, quantity)
					.build();
			}
		}
	}

	interface Playlisttrack {
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
				return entities.builder(TYPE)
					.with(PLAYLISTTRACKID, playlisttrackid)
					.with(PLAYLISTID_FK, playlistid.entity(entities))
					.with(TRACKID_FK, trackid.entity(entities))
					.build();
			}
		}
	}
}