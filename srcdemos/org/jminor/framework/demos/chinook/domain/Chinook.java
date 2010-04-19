package org.jminor.framework.demos.chinook.domain;

import org.jminor.common.db.IdSource;
import org.jminor.common.model.StringProvider;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

public class Chinook {

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

  public static final String T_TRACK = "chinook.track";
  public static final String TRACK_TRACKID = "trackid";
  public static final String TRACK_NAME = "name";
  public static final String TRACK_ALBUMID = "albumid";
  public static final String TRACK_ALBUMID_FK = "albumid_fk";
  public static final String TRACK_MEDIATYPEID = "mediatypeid";
  public static final String TRACK_MEDIATYPEID_FK = "mediatypeid_fk";
  public static final String TRACK_GENREID = "genreid";
  public static final String TRACK_GENREID_FK = "genreid_fk";
  public static final String TRACK_COMPOSER = "composer";
  public static final String TRACK_MILLISECONDS = "milliseconds";
  public static final String TRACK_BYTES = "bytes";
  public static final String TRACK_UNITPRICE = "unitprice";

  static {
    EntityRepository.add(new EntityDefinition(T_ALBUM,
            new Property.PrimaryKeyProperty(ALBUM_ALBUMID),
            new Property(ALBUM_TITLE, Type.STRING, "Title")
                    .setNullable(false)
                    .setMaxLength(160),
            new Property.ForeignKeyProperty(ALBUM_ARTISTID_FK, "Artist", T_ARTIST,
                    new Property(ALBUM_ARTISTID))
                    .setNullable(false))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(ALBUM_TITLE))
            .setLargeDataset(true));

    EntityRepository.add(new EntityDefinition(T_ARTIST,
            new Property.PrimaryKeyProperty(ARTIST_ARTISTID),
            new Property(ARTIST_NAME, Type.STRING, "Name")
                    .setMaxLength(120))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(ARTIST_NAME))
            .setLargeDataset(true));

    EntityRepository.add(new EntityDefinition(T_CUSTOMER,
            new Property.PrimaryKeyProperty(CUSTOMER_CUSTOMERID),
            new Property(CUSTOMER_FIRSTNAME, Type.STRING, "First name")
                    .setNullable(false)
                    .setMaxLength(40),
            new Property(CUSTOMER_LASTNAME, Type.STRING, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            new Property(CUSTOMER_COMPANY, Type.STRING, "Company")
                    .setMaxLength(80),
            new Property(CUSTOMER_ADDRESS, Type.STRING, "Address")
                    .setMaxLength(70),
            new Property(CUSTOMER_CITY, Type.STRING, "City")
                    .setMaxLength(40),
            new Property(CUSTOMER_STATE, Type.STRING, "State")
                    .setMaxLength(40),
            new Property(CUSTOMER_COUNTRY, Type.STRING, "Country")
                    .setMaxLength(40),
            new Property(CUSTOMER_POSTALCODE, Type.STRING, "Postal code")
                    .setMaxLength(10),
            new Property(CUSTOMER_PHONE, Type.STRING, "Phone")
                    .setMaxLength(24),
            new Property(CUSTOMER_FAX, Type.STRING, "Fax")
                    .setMaxLength(24),
            new Property(CUSTOMER_EMAIL, Type.STRING, "Email")
                    .setNullable(false)
                    .setMaxLength(60),
            new Property.ForeignKeyProperty(CUSTOMER_SUPPORTREPID_FK, "Support rep", T_EMPLOYEE,
                    new Property(CUSTOMER_SUPPORTREPID)))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(CUSTOMER_LASTNAME)
            .addText(", ").addValue(CUSTOMER_FIRSTNAME))
            .setLargeDataset(true));

    EntityRepository.add(new EntityDefinition(T_EMPLOYEE,
            new Property.PrimaryKeyProperty(EMPLOYEE_EMPLOYEEID),
            new Property(EMPLOYEE_LASTNAME, Type.STRING, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            new Property(EMPLOYEE_FIRSTNAME, Type.STRING, "First name")
                    .setNullable(false)
                    .setMaxLength(20),
            new Property(EMPLOYEE_TITLE, Type.STRING, "Title")
                    .setMaxLength(30),
            new Property.ForeignKeyProperty(EMPLOYEE_REPORTSTO_FK, "Reports to", T_EMPLOYEE,
                    new Property(EMPLOYEE_REPORTSTO)),
            new Property(EMPLOYEE_BIRTHDATE, Type.DATE, "Birthdate"),
            new Property(EMPLOYEE_HIREDATE, Type.DATE, "Hiredate"),
            new Property(EMPLOYEE_ADDRESS, Type.STRING, "Address")
                    .setMaxLength(70),
            new Property(EMPLOYEE_CITY, Type.STRING, "City")
                    .setMaxLength(40),
            new Property(EMPLOYEE_STATE, Type.STRING, "State")
                    .setMaxLength(40),
            new Property(EMPLOYEE_COUNTRY, Type.STRING, "Country")
                    .setMaxLength(40),
            new Property(EMPLOYEE_POSTALCODE, Type.STRING, "Postal code")
                    .setMaxLength(10),
            new Property(EMPLOYEE_PHONE, Type.STRING, "Phone")
                    .setMaxLength(24),
            new Property(EMPLOYEE_FAX, Type.STRING, "Fax")
                    .setMaxLength(24),
            new Property(EMPLOYEE_EMAIL, Type.STRING, "Email")
                    .setMaxLength(60))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(EMPLOYEE_LASTNAME)
            .addText(", ").addValue(EMPLOYEE_FIRSTNAME)));

    EntityRepository.add(new EntityDefinition(T_GENRE,
            new Property.PrimaryKeyProperty(GENRE_GENREID),
            new Property(GENRE_NAME, Type.STRING, "Name")
                    .setMaxLength(120))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(GENRE_NAME)));

    EntityRepository.add(new EntityDefinition(T_INVOICE,
            new Property.PrimaryKeyProperty(INVOICE_INVOICEID),
            new Property.ForeignKeyProperty(INVOICE_CUSTOMERID_FK, "Customer", T_CUSTOMER,
                    new Property(INVOICE_CUSTOMERID))
                    .setNullable(false),
            new Property(INVOICE_INVOICEDATE, Type.DATE, "Date")
                    .setNullable(false),
            new Property(INVOICE_BILLINGADDRESS, Type.STRING, "Billing address")
                    .setMaxLength(70),
            new Property(INVOICE_BILLINGCITY, Type.STRING, "Billing city")
                    .setMaxLength(40),
            new Property(INVOICE_BILLINGSTATE, Type.STRING, "Billing state")
                    .setMaxLength(40),
            new Property(INVOICE_BILLINGCOUNTRY, Type.STRING, "Billing country")
                    .setMaxLength(40),
            new Property(INVOICE_BILLINGPOSTALCODE, Type.STRING, "Billing postal code")
                    .setMaxLength(10),
            new Property(INVOICE_TOTAL, Type.DOUBLE, "Total")
                    .setNullable(false))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(INVOICE_INVOICEID))
            .setLargeDataset(true));

    EntityRepository.add(new EntityDefinition(T_INVOICELINE,
            new Property.PrimaryKeyProperty(INVOICELINE_INVOICELINEID),
            new Property.ForeignKeyProperty(INVOICELINE_INVOICEID_FK, "Invoice", T_INVOICE,
                    new Property(INVOICELINE_INVOICEID))
                    .setNullable(false),
            new Property.ForeignKeyProperty(INVOICELINE_TRACKID_FK, "Track", T_TRACK,
                    new Property(INVOICELINE_TRACKID))
                    .setNullable(false),
            new Property(INVOICELINE_UNITPRICE, Type.DOUBLE, "Price")
                    .setNullable(false),
            new Property(INVOICELINE_QUANTITY, Type.INT, "Quantity")
                    .setNullable(false))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setLargeDataset(true));

    EntityRepository.add(new EntityDefinition(T_MEDIATYPE,
            new Property.PrimaryKeyProperty(MEDIATYPE_MEDIATYPEID),
            new Property(MEDIATYPE_NAME, Type.STRING, "Name")
                    .setMaxLength(120))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(MEDIATYPE_NAME)));

    EntityRepository.add(new EntityDefinition(T_PLAYLIST,
            new Property.PrimaryKeyProperty(PLAYLIST_PLAYLISTID),
            new Property(PLAYLIST_NAME, Type.STRING, "Name")
                    .setMaxLength(120))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(PLAYLIST_NAME))
            .setLargeDataset(true));

    EntityRepository.add(new EntityDefinition(T_PLAYLISTTRACK,
            new Property.ForeignKeyProperty(PLAYLISTTRACK_PLAYLISTID_FK, "Playlist", T_PLAYLIST,
                    new Property.PrimaryKeyProperty(PLAYLISTTRACK_PLAYLISTID).setUpdatable(true)),
            new Property.ForeignKeyProperty(PLAYLISTTRACK_TRACKID_FK, "Track", T_TRACK,
                    new Property.PrimaryKeyProperty(PLAYLISTTRACK_TRACKID, Type.INT)
                            .setIndex(1).setUpdatable(true)))
            .setStringProvider(new StringProvider<String, Object>(PLAYLISTTRACK_PLAYLISTID_FK)
            .addText(" - ").addValue(PLAYLISTTRACK_TRACKID_FK))
            .setLargeDataset(true));

    EntityRepository.add(new EntityDefinition(T_TRACK,
            new Property.PrimaryKeyProperty(TRACK_TRACKID),
            new Property(TRACK_NAME, Type.STRING, "Name")
                    .setNullable(false)
                    .setMaxLength(200),
            new Property.ForeignKeyProperty(TRACK_ALBUMID_FK, "Album", T_ALBUM,
                    new Property(TRACK_ALBUMID)),
            new Property.ForeignKeyProperty(TRACK_MEDIATYPEID_FK, "Media type", T_MEDIATYPE,
                    new Property(TRACK_MEDIATYPEID))
                    .setNullable(false),
            new Property.ForeignKeyProperty(TRACK_GENREID_FK, "Genre", T_GENRE,
                    new Property(TRACK_GENREID)),
            new Property(TRACK_COMPOSER, Type.STRING, "Composer")
                    .setMaxLength(220),
            new Property(TRACK_MILLISECONDS, Type.INT, "Milliseconds")
                    .setNullable(false),
            new Property(TRACK_BYTES, Type.INT, "Bytes"),
            new Property(TRACK_UNITPRICE, Type.DOUBLE, "Price")
                    .setNullable(false))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setStringProvider(new StringProvider<String, Object>(TRACK_NAME))
            .setLargeDataset(true));
  }
}