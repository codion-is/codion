package is.codion.chinook.domain;

import static is.codion.framework.domain.DomainType.domainType;

import is.codion.framework.domain.DomainType;
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
	}

	interface Album {
		EntityType TYPE = DOMAIN.entityType("chinook.album");

		Column<Long> ALBUMID = TYPE.longColumn("albumid");
		Column<String> TITLE = TYPE.stringColumn("title");
		Column<Long> ARTISTID = TYPE.longColumn("artistid");
		Column<byte[]> COVER = TYPE.byteArrayColumn("cover");
		Column<Object> TAGS = TYPE.column("tags", Object.class);

		ForeignKey ARTISTID_FK = TYPE.foreignKey("artistid_fk", ARTISTID, Artist.ARTISTID);
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
	}

	interface Genre {
		EntityType TYPE = DOMAIN.entityType("chinook.genre");

		Column<Long> GENREID = TYPE.longColumn("genreid");
		Column<String> NAME = TYPE.stringColumn("name");
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
	}

	interface Mediatype {
		EntityType TYPE = DOMAIN.entityType("chinook.mediatype");

		Column<Long> MEDIATYPEID = TYPE.longColumn("mediatypeid");
		Column<String> NAME = TYPE.stringColumn("name");
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
	}

	interface Playlist {
		EntityType TYPE = DOMAIN.entityType("chinook.playlist");

		Column<Long> PLAYLISTID = TYPE.longColumn("playlistid");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	interface Playlisttrack {
		EntityType TYPE = DOMAIN.entityType("chinook.playlisttrack");

		Column<Long> PLAYLISTTRACKID = TYPE.longColumn("playlisttrackid");
		Column<Long> PLAYLISTID = TYPE.longColumn("playlistid");
		Column<Long> TRACKID = TYPE.longColumn("trackid");

		ForeignKey PLAYLISTID_FK = TYPE.foreignKey("playlistid_fk", PLAYLISTID, Playlist.PLAYLISTID);
		ForeignKey TRACKID_FK = TYPE.foreignKey("trackid_fk", TRACKID, Track.TRACKID);
	}

	interface Users {
		EntityType TYPE = DOMAIN.entityType("chinook.users");

		Column<Long> USERID = TYPE.longColumn("userid");
		Column<String> USERNAME = TYPE.stringColumn("username");
		Column<Integer> PASSWORDHASH = TYPE.integerColumn("passwordhash");
	}
}