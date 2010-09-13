package org.jminor.framework.demos.chinook.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.valuemap.StringProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import java.sql.Types;

public class Chinook {

  private Chinook() {}
  public static void init() {}

  public static final String DOMAIN_ID = Chinook.class.getName();

  public static final String T_ALBUM = "chinook.album";
  public static final String ALBUM_ALBUMID = "albumid";
  public static final String ALBUM_TITLE = "title";
  public static final String ALBUM_ARTISTID = "artistid";
  public static final String ALBUM_ARTISTID_FK = "artistid_fk";

  public static final String T_ARTIST = "chinook.artist";
  public static final String ARTIST_ARTISTID = "artistid";
  public static final String ARTIST_NAME = "name";

  public static final String T_CUSTOMER = "chinook.customer";
  public static final String CUSTOMER_CUSTOMERID = "customerid";
  public static final String CUSTOMER_FIRSTNAME = "firstname";
  public static final String CUSTOMER_LASTNAME = "lastname";
  public static final String CUSTOMER_COMPANY = "company";
  public static final String CUSTOMER_ADDRESS = "address";
  public static final String CUSTOMER_CITY = "city";
  public static final String CUSTOMER_STATE = "state";
  public static final String CUSTOMER_COUNTRY = "country";
  public static final String CUSTOMER_POSTALCODE = "postalcode";
  public static final String CUSTOMER_PHONE = "phone";
  public static final String CUSTOMER_FAX = "fax";
  public static final String CUSTOMER_EMAIL = "email";
  public static final String CUSTOMER_SUPPORTREPID = "supportrepid";
  public static final String CUSTOMER_SUPPORTREPID_FK = "supportrepid_fk";

  public static final String T_EMPLOYEE = "chinook.employee";
  public static final String EMPLOYEE_EMPLOYEEID = "employeeid";
  public static final String EMPLOYEE_LASTNAME = "lastname";
  public static final String EMPLOYEE_FIRSTNAME = "firstname";
  public static final String EMPLOYEE_TITLE = "title";
  public static final String EMPLOYEE_REPORTSTO = "reportsto";
  public static final String EMPLOYEE_REPORTSTO_FK = "reportsto_fk";
  public static final String EMPLOYEE_BIRTHDATE = "birthdate";
  public static final String EMPLOYEE_HIREDATE = "hiredate";
  public static final String EMPLOYEE_ADDRESS = "address";
  public static final String EMPLOYEE_CITY = "city";
  public static final String EMPLOYEE_STATE = "state";
  public static final String EMPLOYEE_COUNTRY = "country";
  public static final String EMPLOYEE_POSTALCODE = "postalcode";
  public static final String EMPLOYEE_PHONE = "phone";
  public static final String EMPLOYEE_FAX = "fax";
  public static final String EMPLOYEE_EMAIL = "email";

  public static final String T_GENRE = "chinook.genre";
  public static final String GENRE_GENREID = "genreid";
  public static final String GENRE_NAME = "name";

  public static final String T_INVOICE = "chinook.invoice";
  public static final String INVOICE_INVOICEID = "invoiceid";
  public static final String INVOICE_INVOICEID_AS_STRING = "invoiceid || ''";
  public static final String INVOICE_CUSTOMERID = "customerid";
  public static final String INVOICE_CUSTOMERID_FK = "customerid_fk";
  public static final String INVOICE_INVOICEDATE = "invoicedate";
  public static final String INVOICE_BILLINGADDRESS = "billingaddress";
  public static final String INVOICE_BILLINGCITY = "billingcity";
  public static final String INVOICE_BILLINGSTATE = "billingstate";
  public static final String INVOICE_BILLINGCOUNTRY = "billingcountry";
  public static final String INVOICE_BILLINGPOSTALCODE = "billingpostalcode";
  public static final String INVOICE_TOTAL = "total";

  public static final String T_INVOICELINE = "chinook.invoiceline";
  public static final String INVOICELINE_INVOICELINEID = "invoicelineid";
  public static final String INVOICELINE_INVOICEID = "invoiceid";
  public static final String INVOICELINE_INVOICEID_FK = "invoiceid_fk";
  public static final String INVOICELINE_TRACKID = "trackid";
  public static final String INVOICELINE_TRACKID_FK = "trackid_fk";
  public static final String INVOICELINE_UNITPRICE = "unitprice";
  public static final String INVOICELINE_QUANTITY = "quantity";

  public static final String T_MEDIATYPE = "chinook.mediatype";
  public static final String MEDIATYPE_MEDIATYPEID = "mediatypeid";
  public static final String MEDIATYPE_NAME = "name";

  public static final String T_PLAYLIST = "chinook.playlist";
  public static final String PLAYLIST_PLAYLISTID = "playlistid";
  public static final String PLAYLIST_NAME = "name";

  public static final String T_PLAYLISTTRACK = "chinook.playlisttrack";
  public static final String PLAYLISTTRACK_PLAYLISTID = "playlistid";
  public static final String PLAYLISTTRACK_PLAYLISTID_FK = "playlistid_fk";
  public static final String PLAYLISTTRACK_TRACKID = "trackid";
  public static final String PLAYLISTTRACK_TRACKID_FK = "trackid_fk";
  public static final String PLAYLISTTRACK_ALBUM_DENORM = "album_denorm";
  public static final String PLAYLISTTRACK_ARTIST_DENORM = "artist_denorm";

  public static final String T_TRACK = "chinook.track";
  public static final String TRACK_TRACKID = "trackid";
  public static final String TRACK_NAME = "name";
  public static final String TRACK_ARTIST_DENORM = "artist_denorm";
  public static final String TRACK_ALBUMID = "albumid";
  public static final String TRACK_ALBUMID_FK = "albumid_fk";
  public static final String TRACK_MEDIATYPEID = "mediatypeid";
  public static final String TRACK_MEDIATYPEID_FK = "mediatypeid_fk";
  public static final String TRACK_GENREID = "genreid";
  public static final String TRACK_GENREID_FK = "genreid_fk";
  public static final String TRACK_COMPOSER = "composer";
  public static final String TRACK_MILLISECONDS = "milliseconds";
  public static final String TRACK_MINUTES_SECONDS_DERIVED = "minutes_seconds_transient";
  public static final String TRACK_BYTES = "bytes";
  public static final String TRACK_UNITPRICE = "unitprice";

  public static final Property.DerivedProperty.Provider TRACK_MIN_SEC_PROVIDER =
          new Property.DerivedProperty.Provider() {
            public Object getValue(final Object... linkedValues) {
              final Integer milliseconds = (Integer) linkedValues[0];
              if (milliseconds == null || milliseconds <= 0) {
                return "";
              }

              final int seconds = ((milliseconds / 1000) % 60);
              final int minutes = ((milliseconds / 1000) / 60);

              return minutes + " min " + seconds + " sec";
            }
          };

  static {
    Entities.define(T_ALBUM,
            Properties.primaryKeyProperty(ALBUM_ALBUMID),
            Properties.foreignKeyProperty(ALBUM_ARTISTID_FK, "Artist", T_ARTIST,
                    Properties.columnProperty(ALBUM_ARTISTID))
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            Properties.columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                    .setNullable(false)
                    .setMaxLength(160)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_ALBUM)
            .setStringProvider(new StringProvider<String>(ALBUM_TITLE))
            .setSearchPropertyIDs(ALBUM_TITLE)
            .setOrderByClause(ALBUM_ARTISTID + ", " + ALBUM_TITLE)
            .setCaption("Albums");

    Entities.define(T_ARTIST,
            Properties.primaryKeyProperty(ARTIST_ARTISTID),
            Properties.columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_ARTIST)
            .setStringProvider(new StringProvider<String>(ARTIST_NAME))
            .setSearchPropertyIDs(ARTIST_NAME)
            .setOrderByClause(ARTIST_NAME)
            .setCaption("Artists");

    Entities.define(T_CUSTOMER,
            Properties.primaryKeyProperty(CUSTOMER_CUSTOMERID),
            Properties.columnProperty(CUSTOMER_LASTNAME, Types.VARCHAR, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(CUSTOMER_FIRSTNAME, Types.VARCHAR, "First name")
                    .setNullable(false)
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_COMPANY, Types.VARCHAR, "Company")
                    .setMaxLength(80),
            Properties.columnProperty(CUSTOMER_ADDRESS, Types.VARCHAR, "Address")
                    .setMaxLength(70),
            Properties.columnProperty(CUSTOMER_CITY, Types.VARCHAR, "City")
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_STATE, Types.VARCHAR, "State")
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_COUNTRY, Types.VARCHAR, "Country")
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_POSTALCODE, Types.VARCHAR, "Postal code")
                    .setMaxLength(10),
            Properties.columnProperty(CUSTOMER_PHONE, Types.VARCHAR, "Phone")
                    .setMaxLength(24),
            Properties.columnProperty(CUSTOMER_FAX, Types.VARCHAR, "Fax")
                    .setMaxLength(24),
            Properties.columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email")
                    .setNullable(false)
                    .setMaxLength(60),
            Properties.foreignKeyProperty(CUSTOMER_SUPPORTREPID_FK, "Support rep", T_EMPLOYEE,
                    Properties.columnProperty(CUSTOMER_SUPPORTREPID)))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_CUSTOMER)
            .setStringProvider(new StringProvider<String>(CUSTOMER_LASTNAME)
                    .addText(", ").addValue(CUSTOMER_FIRSTNAME))
            .setSearchPropertyIDs(CUSTOMER_FIRSTNAME, CUSTOMER_LASTNAME, CUSTOMER_EMAIL)
            .setOrderByClause(CUSTOMER_LASTNAME + ", " + CUSTOMER_FIRSTNAME)
            .setCaption("Customers");

    Entities.define(T_EMPLOYEE,
            Properties.primaryKeyProperty(EMPLOYEE_EMPLOYEEID),
            Properties.columnProperty(EMPLOYEE_LASTNAME, Types.VARCHAR, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(EMPLOYEE_FIRSTNAME, Types.VARCHAR, "First name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(EMPLOYEE_TITLE, Types.VARCHAR, "Title")
                    .setMaxLength(30),
            Properties.foreignKeyProperty(EMPLOYEE_REPORTSTO_FK, "Reports to", T_EMPLOYEE,
                    Properties.columnProperty(EMPLOYEE_REPORTSTO)),
            Properties.columnProperty(EMPLOYEE_BIRTHDATE, Types.DATE, "Birthdate"),
            Properties.columnProperty(EMPLOYEE_HIREDATE, Types.DATE, "Hiredate"),
            Properties.columnProperty(EMPLOYEE_ADDRESS, Types.VARCHAR, "Address")
                    .setMaxLength(70),
            Properties.columnProperty(EMPLOYEE_CITY, Types.VARCHAR, "City")
                    .setMaxLength(40),
            Properties.columnProperty(EMPLOYEE_STATE, Types.VARCHAR, "State")
                    .setMaxLength(40),
            Properties.columnProperty(EMPLOYEE_COUNTRY, Types.VARCHAR, "Country")
                    .setMaxLength(40),
            Properties.columnProperty(EMPLOYEE_POSTALCODE, Types.VARCHAR, "Postal code")
                    .setMaxLength(10),
            Properties.columnProperty(EMPLOYEE_PHONE, Types.VARCHAR, "Phone")
                    .setMaxLength(24),
            Properties.columnProperty(EMPLOYEE_FAX, Types.VARCHAR, "Fax")
                    .setMaxLength(24),
            Properties.columnProperty(EMPLOYEE_EMAIL, Types.VARCHAR, "Email")
                    .setMaxLength(60))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_EMPLOYEE)
            .setStringProvider(new StringProvider<String>(EMPLOYEE_LASTNAME)
                    .addText(", ").addValue(EMPLOYEE_FIRSTNAME))
            .setSearchPropertyIDs(EMPLOYEE_FIRSTNAME, EMPLOYEE_LASTNAME, EMPLOYEE_EMAIL)
            .setOrderByClause(EMPLOYEE_LASTNAME + ", " + EMPLOYEE_FIRSTNAME)
            .setCaption("Employees");

    Entities.define(T_GENRE,
            Properties.primaryKeyProperty(GENRE_GENREID),
            Properties.columnProperty(GENRE_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_GENRE)
            .setStringProvider(new StringProvider<String>(GENRE_NAME))
            .setSearchPropertyIDs(GENRE_NAME)
            .setOrderByClause(GENRE_NAME)
            .setCaption("Genres");

    Entities.define(T_MEDIATYPE,
            Properties.primaryKeyProperty(MEDIATYPE_MEDIATYPEID),
            Properties.columnProperty(MEDIATYPE_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_MEDIATYPE)
            .setStringProvider(new StringProvider<String>(MEDIATYPE_NAME))
            .setOrderByClause(MEDIATYPE_NAME)
            .setCaption("Media types");

    Entities.define(T_TRACK,
            Properties.primaryKeyProperty(TRACK_TRACKID),
            Properties.denormalizedViewProperty(TRACK_ARTIST_DENORM, TRACK_ALBUMID_FK,
                    Entities.getProperty(T_ALBUM, ALBUM_ARTISTID_FK), "Artist")
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_ALBUMID_FK, "Album", T_ALBUM,
                    Properties.columnProperty(TRACK_ALBUMID))
                    .setFetchDepth(2)
                    .setPreferredColumnWidth(160),
            Properties.columnProperty(TRACK_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(200)
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_GENREID_FK, "Genre", T_GENRE,
                    Properties.columnProperty(TRACK_GENREID)),
            Properties.columnProperty(TRACK_COMPOSER, Types.VARCHAR, "Composer")
                    .setMaxLength(220)
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_MEDIATYPEID_FK, "Media type", T_MEDIATYPE,
                    Properties.columnProperty(TRACK_MEDIATYPEID))
                    .setNullable(false),
            Properties.columnProperty(TRACK_MILLISECONDS, Types.INTEGER, "Duration (ms)")
                    .setNullable(false),
            Properties.derivedProperty(TRACK_MINUTES_SECONDS_DERIVED, Types.VARCHAR, "Duration (min/sec)",
                    TRACK_MIN_SEC_PROVIDER, TRACK_MILLISECONDS),
            Properties.columnProperty(TRACK_BYTES, Types.INTEGER, "Bytes"),
            Properties.columnProperty(TRACK_UNITPRICE, Types.DOUBLE, "Price")
                    .setNullable(false))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_TRACK)
            .setStringProvider(new StringProvider<String>(TRACK_NAME))
            .setSearchPropertyIDs(TRACK_NAME)
            .setOrderByClause(TRACK_NAME)
            .setCaption("Tracks");

    Entities.define(T_PLAYLIST,
            Properties.primaryKeyProperty(PLAYLIST_PLAYLISTID),
            Properties.columnProperty(PLAYLIST_NAME, Types.VARCHAR, "Name")
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_PLAYLIST)
            .setStringProvider(new StringProvider<String>(PLAYLIST_NAME))
            .setSearchPropertyIDs(PLAYLIST_NAME)
            .setOrderByClause(PLAYLIST_NAME)
            .setCaption("Playlists");

    Entities.define(T_PLAYLISTTRACK,
            Properties.foreignKeyProperty(PLAYLISTTRACK_PLAYLISTID_FK, "Playlist", T_PLAYLIST,
                    Properties.primaryKeyProperty(PLAYLISTTRACK_PLAYLISTID)
                            .setUpdatable(true))
                    .setNullable(false)
                    .setPreferredColumnWidth(120),
            Properties.denormalizedViewProperty(PLAYLISTTRACK_ARTIST_DENORM, PLAYLISTTRACK_ALBUM_DENORM,
                    Entities.getProperty(T_ALBUM, ALBUM_ARTISTID_FK), "Artist")
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(PLAYLISTTRACK_TRACKID_FK, "Track", T_TRACK,
                    Properties.primaryKeyProperty(PLAYLISTTRACK_TRACKID, Types.INTEGER)
                            .setIndex(1)
                            .setUpdatable(true))
                    .setFetchDepth(3)
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            Properties.denormalizedViewProperty(PLAYLISTTRACK_ALBUM_DENORM, PLAYLISTTRACK_TRACKID_FK,
                    Entities.getProperty(T_TRACK, TRACK_ALBUMID_FK), "Album")
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.NONE)
            .setStringProvider(new StringProvider<String>(PLAYLISTTRACK_PLAYLISTID_FK)
                    .addText(" - ").addValue(PLAYLISTTRACK_TRACKID_FK))
            .setCaption("Playlist tracks");

    Entities.define(T_INVOICE,
            Properties.primaryKeyProperty(INVOICE_INVOICEID, Types.INTEGER, "Invoice no."),
            Properties.columnProperty(INVOICE_INVOICEID_AS_STRING, Types.VARCHAR, "Invoice no.")
                    .setReadOnly(true)
                    .setHidden(true),
            Properties.foreignKeyProperty(INVOICE_CUSTOMERID_FK, "Customer", T_CUSTOMER,
                    Properties.columnProperty(INVOICE_CUSTOMERID))
                    .setNullable(false),
            Properties.columnProperty(INVOICE_INVOICEDATE, Types.DATE, "Date")
                    .setNullable(false),
            Properties.columnProperty(INVOICE_BILLINGADDRESS, Types.VARCHAR, "Billing address")
                    .setMaxLength(70),
            Properties.columnProperty(INVOICE_BILLINGCITY, Types.VARCHAR, "Billing city")
                    .setMaxLength(40),
            Properties.columnProperty(INVOICE_BILLINGSTATE, Types.VARCHAR, "Billing state")
                    .setMaxLength(40),
            Properties.columnProperty(INVOICE_BILLINGCOUNTRY, Types.VARCHAR, "Billing country")
                    .setMaxLength(40),
            Properties.columnProperty(INVOICE_BILLINGPOSTALCODE, Types.VARCHAR, "Billing postal code")
                    .setMaxLength(10),
            Properties.columnProperty(INVOICE_TOTAL, Types.DOUBLE, "Total")
                    .setNullable(false))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_INVOICE)
            .setStringProvider(new StringProvider<String>(INVOICE_INVOICEID))
            .setSearchPropertyIDs(INVOICE_INVOICEID_AS_STRING)
            .setOrderByClause(INVOICE_CUSTOMERID + ", " + INVOICE_INVOICEDATE + " desc")
            .setCaption("Invoices");

    Entities.define(T_INVOICELINE,
            Properties.primaryKeyProperty(INVOICELINE_INVOICELINEID),
            Properties.foreignKeyProperty(INVOICELINE_INVOICEID_FK, "Invoice", T_INVOICE,
                    Properties.columnProperty(INVOICELINE_INVOICEID))
                    .setNullable(false),
            Properties.foreignKeyProperty(INVOICELINE_TRACKID_FK, "Track", T_TRACK,
                    Properties.columnProperty(INVOICELINE_TRACKID))
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            Properties.denormalizedProperty(INVOICELINE_UNITPRICE, INVOICELINE_TRACKID_FK,
                    Entities.getProperty(T_TRACK, TRACK_UNITPRICE), "Unit price")
                    .setNullable(false),
            Properties.columnProperty(INVOICELINE_QUANTITY, Types.INTEGER, "Quantity")
                    .setNullable(false))
            .setDomainID(DOMAIN_ID)
            .setIdSource(IdSource.AUTO_INCREMENT).setIdValueSource(T_INVOICELINE)
            .setCaption("Invoice lines");
  }
}