/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.domain;

import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Properties;

import java.sql.Types;
import java.text.NumberFormat;
import java.util.List;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public final class ChinookDomain extends Entities {

  public ChinookDomain() {
    artist();
    album();
    employee();
    customer();
    genre();
    mediaType();
    track();
    invoice();
    invoiceLine();
    playlist();
    playlistTrack();
    dbOperations();
  }

  void artist() {
    define(T_ARTIST, "chinook.artist",
            Properties.primaryKeyProperty(ARTIST_ARTISTID, Types.BIGINT),
            Properties.columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.artist"))
            .setOrderBy(orderBy().ascending(ARTIST_NAME))
            .setStringProvider(new Entities.StringProvider(ARTIST_NAME))
            .setSearchPropertyIds(ARTIST_NAME)
            .setCaption("Artists");
  }

  void album() {
    define(T_ALBUM, "chinook.album",
            Properties.primaryKeyProperty(ALBUM_ALBUMID, Types.BIGINT),
            Properties.foreignKeyProperty(ALBUM_ARTISTID_FK, "Artist", T_ARTIST,
                    Properties.columnProperty(ALBUM_ARTISTID, Types.BIGINT))
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            Properties.columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                    .setNullable(false)
                    .setMaxLength(160)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.album"))
            .setOrderBy(orderBy().ascending(ALBUM_ARTISTID, ALBUM_TITLE))
            .setStringProvider(new Entities.StringProvider(ALBUM_TITLE))
            .setSearchPropertyIds(ALBUM_TITLE)
            .setCaption("Albums");
  }

  void employee() {
    define(T_EMPLOYEE, "chinook.employee",
            Properties.primaryKeyProperty(EMPLOYEE_EMPLOYEEID, Types.BIGINT),
            Properties.columnProperty(EMPLOYEE_LASTNAME, Types.VARCHAR, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(EMPLOYEE_FIRSTNAME, Types.VARCHAR, "First name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(EMPLOYEE_TITLE, Types.VARCHAR, "Title")
                    .setMaxLength(30),
            Properties.foreignKeyProperty(EMPLOYEE_REPORTSTO_FK, "Reports to", T_EMPLOYEE,
                    Properties.columnProperty(EMPLOYEE_REPORTSTO, Types.BIGINT)),
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
            .setKeyGenerator(automaticKeyGenerator("chinook.employee"))
            .setOrderBy(orderBy().ascending(EMPLOYEE_LASTNAME, EMPLOYEE_FIRSTNAME))
            .setStringProvider(new Entities.StringProvider(EMPLOYEE_LASTNAME)
                    .addText(", ").addValue(EMPLOYEE_FIRSTNAME))
            .setSearchPropertyIds(EMPLOYEE_FIRSTNAME, EMPLOYEE_LASTNAME, EMPLOYEE_EMAIL)
            .setCaption("Employees");
  }

  void customer() {
    define(T_CUSTOMER, "chinook.customer",
            Properties.primaryKeyProperty(CUSTOMER_CUSTOMERID, Types.BIGINT),
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
                    Properties.columnProperty(CUSTOMER_SUPPORTREPID, Types.BIGINT)))
            .setKeyGenerator(automaticKeyGenerator("chinook.customer"))
            .setOrderBy(orderBy().ascending(CUSTOMER_LASTNAME, CUSTOMER_FIRSTNAME))
            .setStringProvider(new Entities.StringProvider(CUSTOMER_LASTNAME)
                    .addText(", ").addValue(CUSTOMER_FIRSTNAME))
            .setSearchPropertyIds(CUSTOMER_FIRSTNAME, CUSTOMER_LASTNAME, CUSTOMER_EMAIL)
            .setCaption("Customers");
  }

  void genre() {
    define(T_GENRE, "chinook.genre",
            Properties.primaryKeyProperty(GENRE_GENREID, Types.BIGINT),
            Properties.columnProperty(GENRE_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.genre"))
            .setOrderBy(orderBy().ascending(GENRE_NAME))
            .setStringProvider(new Entities.StringProvider(GENRE_NAME))
            .setSearchPropertyIds(GENRE_NAME)
            .setSmallDataset(true)
            .setCaption("Genres");
  }

  void mediaType() {
    define(T_MEDIATYPE, "chinook.mediatype",
            Properties.primaryKeyProperty(MEDIATYPE_MEDIATYPEID, Types.BIGINT),
            Properties.columnProperty(MEDIATYPE_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.mediatype"))
            .setStringProvider(new Entities.StringProvider(MEDIATYPE_NAME))
            .setSmallDataset(true)
            .setCaption("Media types");
  }

  void track() {
    define(T_TRACK, "chinook.track",
            Properties.primaryKeyProperty(TRACK_TRACKID, Types.BIGINT),
            Properties.denormalizedViewProperty(TRACK_ARTIST_DENORM, TRACK_ALBUMID_FK,
                    getProperty(T_ALBUM, ALBUM_ARTISTID_FK), "Artist")
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_ALBUMID_FK, "Album", T_ALBUM,
                    Properties.columnProperty(TRACK_ALBUMID, Types.BIGINT))
                    .setFetchDepth(2)
                    .setPreferredColumnWidth(160),
            Properties.columnProperty(TRACK_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(200)
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_GENREID_FK, "Genre", T_GENRE,
                    Properties.columnProperty(TRACK_GENREID, Types.BIGINT)),
            Properties.columnProperty(TRACK_COMPOSER, Types.VARCHAR, "Composer")
                    .setMaxLength(220)
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_MEDIATYPEID_FK, "Media type", T_MEDIATYPE,
                    Properties.columnProperty(TRACK_MEDIATYPEID, Types.BIGINT))
                    .setNullable(false),
            Properties.columnProperty(TRACK_MILLISECONDS, Types.INTEGER, "Duration (ms)")
                    .setNullable(false)
                    .setFormat(NumberFormat.getIntegerInstance()),
            Properties.derivedProperty(TRACK_MINUTES_SECONDS_DERIVED, Types.VARCHAR, "Duration (min/sec)",
                    TRACK_MIN_SEC_PROVIDER, TRACK_MILLISECONDS),
            Properties.columnProperty(TRACK_BYTES, Types.INTEGER, "Bytes")
                    .setFormat(NumberFormat.getIntegerInstance()),
            Properties.columnProperty(TRACK_UNITPRICE, Types.DOUBLE, "Price")
                    .setNullable(false))
            .setKeyGenerator(automaticKeyGenerator("chinook.track"))
            .setOrderBy(orderBy().ascending(TRACK_NAME))
            .setStringProvider(new Entities.StringProvider(TRACK_NAME))
            .setSearchPropertyIds(TRACK_NAME)
            .setCaption("Tracks");
  }
  public static final String INVOICE_TOTAL_SUBQUERY = "select sum(unitprice * quantity) from chinook.invoiceline where invoiceid = invoice.invoiceid";

  void invoice() {
    define(T_INVOICE, "chinook.invoice",
            Properties.primaryKeyProperty(INVOICE_INVOICEID, Types.BIGINT, "Invoice no."),
            Properties.columnProperty(INVOICE_INVOICEID_AS_STRING, Types.VARCHAR, "Invoice no.")
                    .setReadOnly(true)
                    .setHidden(true),
            Properties.foreignKeyProperty(INVOICE_CUSTOMERID_FK, "Customer", T_CUSTOMER,
                    Properties.columnProperty(INVOICE_CUSTOMERID, Types.BIGINT))
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
                    .setMaximumFractionDigits(2)
                    .setHidden(true),
            Properties.subqueryProperty(INVOICE_TOTAL_SUB, Types.DOUBLE, "Calculated total", INVOICE_TOTAL_SUBQUERY)
                    .setMaximumFractionDigits(2))
            .setKeyGenerator(automaticKeyGenerator("chinook.invoice"))
            .setOrderBy(orderBy().ascending(INVOICE_CUSTOMERID).descending(INVOICE_INVOICEDATE))
            .setStringProvider(new Entities.StringProvider(INVOICE_INVOICEID))
            .setSearchPropertyIds(INVOICE_INVOICEID_AS_STRING)
            .setCaption("Invoices");
  }

  void invoiceLine() {
    define(T_INVOICELINE, "chinook.invoiceline",
            Properties.primaryKeyProperty(INVOICELINE_INVOICELINEID, Types.BIGINT),
            Properties.foreignKeyProperty(INVOICELINE_INVOICEID_FK, "Invoice", T_INVOICE,
                    Properties.columnProperty(INVOICELINE_INVOICEID, Types.BIGINT))
                    .setFetchDepth(0)
                    .setNullable(false),
            Properties.foreignKeyProperty(INVOICELINE_TRACKID_FK, "Track", T_TRACK,
                    Properties.columnProperty(INVOICELINE_TRACKID, Types.BIGINT))
                    .setNullable(false)
                    .setPreferredColumnWidth(100),
            Properties.denormalizedProperty(INVOICELINE_UNITPRICE, INVOICELINE_TRACKID_FK,
                    getProperty(T_TRACK, TRACK_UNITPRICE), "Unit price")
                    .setNullable(false),
            Properties.columnProperty(INVOICELINE_QUANTITY, Types.INTEGER, "Quantity")
                    .setNullable(false),
            Properties.derivedProperty(INVOICELINE_TOTAL, Types.DOUBLE, "Total", INVOICELINE_TOTAL_PROVIDER,
                    INVOICELINE_QUANTITY, INVOICELINE_UNITPRICE))
            .setKeyGenerator(automaticKeyGenerator("chinook.invoiceline"))
            .setCaption("Invoice lines");
  }

  void playlist() {
    define(T_PLAYLIST, "chinook.playlist",
            Properties.primaryKeyProperty(PLAYLIST_PLAYLISTID, Types.BIGINT),
            Properties.columnProperty(PLAYLIST_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.playlist"))
            .setOrderBy(orderBy().ascending(PLAYLIST_NAME))
            .setStringProvider(new Entities.StringProvider(PLAYLIST_NAME))
            .setSearchPropertyIds(PLAYLIST_NAME)
            .setCaption("Playlists");
  }

  void playlistTrack() {
    define(T_PLAYLISTTRACK, "chinook.playlisttrack",
            Properties.foreignKeyProperty(PLAYLISTTRACK_PLAYLISTID_FK, "Playlist", T_PLAYLIST,
                    Properties.primaryKeyProperty(PLAYLISTTRACK_PLAYLISTID, Types.BIGINT)
                            .setUpdatable(true))
                    .setNullable(false)
                    .setPreferredColumnWidth(120),
            Properties.denormalizedViewProperty(PLAYLISTTRACK_ARTIST_DENORM, PLAYLISTTRACK_ALBUM_DENORM,
                    getProperty(T_ALBUM, ALBUM_ARTISTID_FK), "Artist")
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(PLAYLISTTRACK_TRACKID_FK, "Track", T_TRACK,
                    Properties.primaryKeyProperty(PLAYLISTTRACK_TRACKID, Types.BIGINT)
                            .setPrimaryKeyIndex(1)
                            .setUpdatable(true))
                    .setFetchDepth(3)
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            Properties.denormalizedViewProperty(PLAYLISTTRACK_ALBUM_DENORM, PLAYLISTTRACK_TRACKID_FK,
                    getProperty(T_TRACK, TRACK_ALBUMID_FK), "Album")
                    .setPreferredColumnWidth(160))
            .setStringProvider(new Entities.StringProvider(PLAYLISTTRACK_PLAYLISTID_FK)
                    .addText(" - ").addValue(PLAYLISTTRACK_TRACKID_FK))
            .setCaption("Playlist tracks");
  }

  void dbOperations() {
    addOperation(new UpdateTotalsProcedure(P_UPDATE_TOTALS));
  }

  private static final class UpdateTotalsProcedure extends AbstractProcedure<LocalEntityConnection> {

    private UpdateTotalsProcedure(final String id) {
      super(id, "Update invoice totals");
    }

    @Override
    public void execute(final LocalEntityConnection entityConnection, final Object... arguments) throws DatabaseException {
      try {
        entityConnection.beginTransaction();
        final EntitySelectCondition selectCondition = new EntityConditions(entityConnection.getDomain()).selectCondition(Chinook.T_INVOICE);
        selectCondition.setForUpdate(true);
        selectCondition.setForeignKeyFetchDepthLimit(0);
        final List<Entity> invoices = entityConnection.selectMany(selectCondition);
        for (final Entity invoice : invoices) {
          invoice.put(Chinook.INVOICE_TOTAL, invoice.get(Chinook.INVOICE_TOTAL_SUB));
        }
        final List<Entity> modifiedInvoices = Entities.getModifiedEntities(invoices);
        if (!modifiedInvoices.isEmpty()) {
          entityConnection.update(modifiedInvoices);
        }
        entityConnection.commitTransaction();
      }
      catch (final DatabaseException dbException) {
        if (entityConnection.isTransactionOpen()) {
          entityConnection.rollbackTransaction();
        }
        throw dbException;
      }
    }
  }
}