/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.domain.impl;

import org.jminor.common.db.AbstractFunction;
import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.ConditionType;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.StringProvider;
import org.jminor.framework.domain.property.DerivedProperty;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;
import static org.jminor.framework.domain.Entities.getModifiedEntities;
import static org.jminor.framework.domain.property.Properties.*;

public final class ChinookImpl extends Domain implements Chinook {

  public ChinookImpl() {
    user();
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

  void user() {
    define(T_USER, "chinook.user",
            primaryKeyProperty(USER_USERID),
            columnProperty(USER_USERNAME, Types.VARCHAR, "Username")
                    .setNullable(false)
                    .setMaxLength(20),
            columnProperty(USER_PASSWORD_HASH, Types.INTEGER, "Password hash"))
            .setKeyGenerator(automaticKeyGenerator("chinook.user"))
            .setOrderBy(orderBy().ascending(USER_USERNAME))
            .setStringProvider(new StringProvider(USER_USERNAME))
            .setSearchPropertyIds(USER_USERNAME)
            .setCaption("Users");
  }

  void artist() {
    define(T_ARTIST, "chinook.artist",
            primaryKeyProperty(ARTIST_ARTISTID, Types.BIGINT),
            columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.artist"))
            .setOrderBy(orderBy().ascending(ARTIST_NAME))
            .setStringProvider(new StringProvider(ARTIST_NAME))
            .setSearchPropertyIds(ARTIST_NAME)
            .setCaption("Artists");
  }

  void album() {
    define(T_ALBUM, "chinook.album",
            primaryKeyProperty(ALBUM_ALBUMID, Types.BIGINT),
            foreignKeyProperty(ALBUM_ARTIST_FK, "Artist", T_ARTIST,
                    columnProperty(ALBUM_ARTISTID, Types.BIGINT))
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                    .setNullable(false)
                    .setMaxLength(160)
                    .setPreferredColumnWidth(160),
            columnProperty(ALBUM_COVERART, Types.BLOB),
            derivedProperty(ALBUM_COVERART_IMAGE, Types.JAVA_OBJECT, null,
                    new CoverArtImageProvider(), ALBUM_COVERART))
            .setKeyGenerator(automaticKeyGenerator("chinook.album"))
            .setOrderBy(orderBy().ascending(ALBUM_ARTISTID, ALBUM_TITLE))
            .setStringProvider(new StringProvider(ALBUM_TITLE))
            .setSearchPropertyIds(ALBUM_TITLE)
            .setCaption("Albums");
  }

  void employee() {
    define(T_EMPLOYEE, "chinook.employee",
            primaryKeyProperty(EMPLOYEE_EMPLOYEEID, Types.BIGINT),
            columnProperty(EMPLOYEE_LASTNAME, Types.VARCHAR, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            columnProperty(EMPLOYEE_FIRSTNAME, Types.VARCHAR, "First name")
                    .setNullable(false)
                    .setMaxLength(20),
            columnProperty(EMPLOYEE_TITLE, Types.VARCHAR, "Title")
                    .setMaxLength(30),
            foreignKeyProperty(EMPLOYEE_REPORTSTO_FK, "Reports to", T_EMPLOYEE,
                    columnProperty(EMPLOYEE_REPORTSTO, Types.BIGINT)),
            columnProperty(EMPLOYEE_BIRTHDATE, Types.DATE, "Birthdate"),
            columnProperty(EMPLOYEE_HIREDATE, Types.DATE, "Hiredate"),
            columnProperty(EMPLOYEE_ADDRESS, Types.VARCHAR, "Address")
                    .setMaxLength(70),
            columnProperty(EMPLOYEE_CITY, Types.VARCHAR, "City")
                    .setMaxLength(40),
            columnProperty(EMPLOYEE_STATE, Types.VARCHAR, "State")
                    .setMaxLength(40),
            columnProperty(EMPLOYEE_COUNTRY, Types.VARCHAR, "Country")
                    .setMaxLength(40),
            columnProperty(EMPLOYEE_POSTALCODE, Types.VARCHAR, "Postal code")
                    .setMaxLength(10),
            columnProperty(EMPLOYEE_PHONE, Types.VARCHAR, "Phone")
                    .setMaxLength(24),
            columnProperty(EMPLOYEE_FAX, Types.VARCHAR, "Fax")
                    .setMaxLength(24),
            columnProperty(EMPLOYEE_EMAIL, Types.VARCHAR, "Email")
                    .setMaxLength(60))
            .setKeyGenerator(automaticKeyGenerator("chinook.employee"))
            .setOrderBy(orderBy().ascending(EMPLOYEE_LASTNAME, EMPLOYEE_FIRSTNAME))
            .setStringProvider(new StringProvider(EMPLOYEE_LASTNAME)
                    .addText(", ").addValue(EMPLOYEE_FIRSTNAME))
            .setSearchPropertyIds(EMPLOYEE_FIRSTNAME, EMPLOYEE_LASTNAME, EMPLOYEE_EMAIL)
            .setCaption("Employees");
  }

  void customer() {
    define(T_CUSTOMER, "chinook.customer",
            primaryKeyProperty(CUSTOMER_CUSTOMERID, Types.BIGINT),
            columnProperty(CUSTOMER_LASTNAME, Types.VARCHAR, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            columnProperty(CUSTOMER_FIRSTNAME, Types.VARCHAR, "First name")
                    .setNullable(false)
                    .setMaxLength(40),
            columnProperty(CUSTOMER_COMPANY, Types.VARCHAR, "Company")
                    .setMaxLength(80),
            columnProperty(CUSTOMER_ADDRESS, Types.VARCHAR, "Address")
                    .setMaxLength(70),
            columnProperty(CUSTOMER_CITY, Types.VARCHAR, "City")
                    .setMaxLength(40),
            columnProperty(CUSTOMER_STATE, Types.VARCHAR, "State")
                    .setMaxLength(40),
            columnProperty(CUSTOMER_COUNTRY, Types.VARCHAR, "Country")
                    .setMaxLength(40),
            columnProperty(CUSTOMER_POSTALCODE, Types.VARCHAR, "Postal code")
                    .setMaxLength(10),
            columnProperty(CUSTOMER_PHONE, Types.VARCHAR, "Phone")
                    .setMaxLength(24),
            columnProperty(CUSTOMER_FAX, Types.VARCHAR, "Fax")
                    .setMaxLength(24),
            columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email")
                    .setNullable(false)
                    .setMaxLength(60),
            foreignKeyProperty(CUSTOMER_SUPPORTREP_FK, "Support rep", T_EMPLOYEE,
                    columnProperty(CUSTOMER_SUPPORTREPID, Types.BIGINT)))
            .setKeyGenerator(automaticKeyGenerator("chinook.customer"))
            .setOrderBy(orderBy().ascending(CUSTOMER_LASTNAME, CUSTOMER_FIRSTNAME))
            .setStringProvider(customer -> {
              final StringBuilder builder =
                      new StringBuilder(customer.getString(CUSTOMER_LASTNAME))
                              .append(", ").append(customer.getString(CUSTOMER_FIRSTNAME));
              if (customer.isNotNull(CUSTOMER_EMAIL)) {
                builder.append(" <").append(customer.getString(CUSTOMER_EMAIL)).append(">");
              }

              return builder.toString();
            })
            .setSearchPropertyIds(CUSTOMER_FIRSTNAME, CUSTOMER_LASTNAME, CUSTOMER_EMAIL)
            .setCaption("Customers");
  }

  void genre() {
    define(T_GENRE, "chinook.genre",
            primaryKeyProperty(GENRE_GENREID, Types.BIGINT),
            columnProperty(GENRE_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.genre"))
            .setOrderBy(orderBy().ascending(GENRE_NAME))
            .setStringProvider(new StringProvider(GENRE_NAME))
            .setSearchPropertyIds(GENRE_NAME)
            .setSmallDataset(true)
            .setCaption("Genres");
  }

  void mediaType() {
    define(T_MEDIATYPE, "chinook.mediatype",
            primaryKeyProperty(MEDIATYPE_MEDIATYPEID, Types.BIGINT),
            columnProperty(MEDIATYPE_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.mediatype"))
            .setStringProvider(new StringProvider(MEDIATYPE_NAME))
            .setSmallDataset(true)
            .setCaption("Media types");
  }

  void track() {
    define(T_TRACK, "chinook.track",
            primaryKeyProperty(TRACK_TRACKID, Types.BIGINT),
            denormalizedViewProperty(TRACK_ARTIST_DENORM, TRACK_ALBUM_FK,
                    getDefinition(T_ALBUM).getProperty(ALBUM_ARTIST_FK), "Artist")
                    .setPreferredColumnWidth(160),
            foreignKeyProperty(TRACK_ALBUM_FK, "Album", T_ALBUM,
                    columnProperty(TRACK_ALBUMID, Types.BIGINT))
                    .setFetchDepth(2)
                    .setPreferredColumnWidth(160),
            columnProperty(TRACK_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(200)
                    .setPreferredColumnWidth(160),
            foreignKeyProperty(TRACK_GENRE_FK, "Genre", T_GENRE,
                    columnProperty(TRACK_GENREID, Types.BIGINT)),
            columnProperty(TRACK_COMPOSER, Types.VARCHAR, "Composer")
                    .setMaxLength(220)
                    .setPreferredColumnWidth(160),
            foreignKeyProperty(TRACK_MEDIATYPE_FK, "Media type", T_MEDIATYPE,
                    columnProperty(TRACK_MEDIATYPEID, Types.BIGINT))
                    .setNullable(false),
            columnProperty(TRACK_MILLISECONDS, Types.INTEGER, "Duration (ms)")
                    .setNullable(false)
                    .setFormat(NumberFormat.getIntegerInstance()),
            derivedProperty(TRACK_MINUTES_SECONDS_DERIVED, Types.VARCHAR, "Duration (min/sec)",
                    TRACK_MIN_SEC_PROVIDER, TRACK_MILLISECONDS),
            columnProperty(TRACK_BYTES, Types.INTEGER, "Bytes")
                    .setFormat(NumberFormat.getIntegerInstance()),
            columnProperty(TRACK_UNITPRICE, Types.DECIMAL, "Price")
                    .setNullable(false)
                    .setMaximumFractionDigits(2))
            .setKeyGenerator(automaticKeyGenerator("chinook.track"))
            .setOrderBy(orderBy().ascending(TRACK_NAME))
            .setStringProvider(new StringProvider(TRACK_NAME))
            .setSearchPropertyIds(TRACK_NAME)
            .setCaption("Tracks");
  }

  void invoice() {
    define(T_INVOICE, "chinook.invoice",
            primaryKeyProperty(INVOICE_INVOICEID, Types.BIGINT, "Invoice no."),
            columnProperty(INVOICE_INVOICEID_AS_STRING, Types.VARCHAR, "Invoice no.")
                    .setReadOnly(true)
                    .setHidden(true),
            foreignKeyProperty(INVOICE_CUSTOMER_FK, "Customer", T_CUSTOMER,
                    columnProperty(INVOICE_CUSTOMERID, Types.BIGINT))
                    .setNullable(false),
            columnProperty(INVOICE_INVOICEDATE, Types.TIMESTAMP, "Date/time")
                    .setNullable(false),
            columnProperty(INVOICE_BILLINGADDRESS, Types.VARCHAR, "Billing address")
                    .setMaxLength(70),
            columnProperty(INVOICE_BILLINGCITY, Types.VARCHAR, "Billing city")
                    .setMaxLength(40),
            columnProperty(INVOICE_BILLINGSTATE, Types.VARCHAR, "Billing state")
                    .setMaxLength(40),
            columnProperty(INVOICE_BILLINGCOUNTRY, Types.VARCHAR, "Billing country")
                    .setMaxLength(40),
            columnProperty(INVOICE_BILLINGPOSTALCODE, Types.VARCHAR, "Billing postal code")
                    .setMaxLength(10),
            columnProperty(INVOICE_TOTAL, Types.DECIMAL, "Total")
                    .setMaximumFractionDigits(2)
                    .setHidden(true),
            subqueryProperty(INVOICE_TOTAL_SUB, Types.DECIMAL, "Calculated total",
                    "select sum(unitprice * quantity) from chinook.invoiceline " +
                            "where invoiceid = invoice.invoiceid")
                    .setMaximumFractionDigits(2))
            .setKeyGenerator(automaticKeyGenerator("chinook.invoice"))
            .setOrderBy(orderBy().ascending(INVOICE_CUSTOMERID).descending(INVOICE_INVOICEDATE))
            .setStringProvider(new StringProvider(INVOICE_INVOICEID))
            .setSearchPropertyIds(INVOICE_INVOICEID_AS_STRING)
            .setCaption("Invoices");
  }

  void invoiceLine() {
    define(T_INVOICELINE, "chinook.invoiceline",
            primaryKeyProperty(INVOICELINE_INVOICELINEID, Types.BIGINT),
            foreignKeyProperty(INVOICELINE_INVOICE_FK, "Invoice", T_INVOICE,
                    columnProperty(INVOICELINE_INVOICEID, Types.BIGINT))
                    .setFetchDepth(0)
                    .setNullable(false),
            foreignKeyProperty(INVOICELINE_TRACK_FK, "Track", T_TRACK,
                    columnProperty(INVOICELINE_TRACKID, Types.BIGINT))
                    .setNullable(false)
                    .setPreferredColumnWidth(100),
            denormalizedProperty(INVOICELINE_UNITPRICE, INVOICELINE_TRACK_FK,
                    getDefinition(T_TRACK).getProperty(TRACK_UNITPRICE), "Unit price")
                    .setNullable(false),
            columnProperty(INVOICELINE_QUANTITY, Types.INTEGER, "Quantity")
                    .setNullable(false),
            derivedProperty(INVOICELINE_TOTAL, Types.DOUBLE, "Total", INVOICELINE_TOTAL_PROVIDER,
                    INVOICELINE_QUANTITY, INVOICELINE_UNITPRICE))
            .setKeyGenerator(automaticKeyGenerator("chinook.invoiceline"))
            .setCaption("Invoice lines");
  }

  void playlist() {
    define(T_PLAYLIST, "chinook.playlist",
            primaryKeyProperty(PLAYLIST_PLAYLISTID, Types.BIGINT),
            columnProperty(PLAYLIST_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setKeyGenerator(automaticKeyGenerator("chinook.playlist"))
            .setOrderBy(orderBy().ascending(PLAYLIST_NAME))
            .setStringProvider(new StringProvider(PLAYLIST_NAME))
            .setSearchPropertyIds(PLAYLIST_NAME)
            .setCaption("Playlists");
  }

  void playlistTrack() {
    define(T_PLAYLISTTRACK, "chinook.playlisttrack",
            foreignKeyProperty(PLAYLISTTRACK_PLAYLIST_FK, "Playlist", T_PLAYLIST,
                    primaryKeyProperty(PLAYLISTTRACK_PLAYLISTID, Types.BIGINT)
                            .setUpdatable(true))
                    .setNullable(false)
                    .setPreferredColumnWidth(120),
            denormalizedViewProperty(PLAYLISTTRACK_ARTIST_DENORM, PLAYLISTTRACK_ALBUM_DENORM,
                    getDefinition(T_ALBUM).getProperty(ALBUM_ARTIST_FK), "Artist")
                    .setPreferredColumnWidth(160),
            foreignKeyProperty(PLAYLISTTRACK_TRACK_FK, "Track", T_TRACK,
                    primaryKeyProperty(PLAYLISTTRACK_TRACKID, Types.BIGINT)
                            .setPrimaryKeyIndex(1)
                            .setUpdatable(true))
                    .setFetchDepth(3)
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            denormalizedViewProperty(PLAYLISTTRACK_ALBUM_DENORM, PLAYLISTTRACK_TRACK_FK,
                    getDefinition(T_TRACK).getProperty(TRACK_ALBUM_FK), "Album")
                    .setPreferredColumnWidth(160))
            .setStringProvider(new StringProvider(PLAYLISTTRACK_PLAYLIST_FK)
                    .addText(" - ").addValue(PLAYLISTTRACK_TRACK_FK))
            .setCaption("Playlist tracks");
  }

  void dbOperations() {
    addOperation(new UpdateTotalsProcedure());
    addOperation(new RaisePriceFunction());
  }

  private static final class UpdateTotalsProcedure extends AbstractProcedure<LocalEntityConnection> {

    private UpdateTotalsProcedure() {
      super(P_UPDATE_TOTALS, "Update invoice totals");
    }

    @Override
    public void execute(final LocalEntityConnection entityConnection,
                        final Object... arguments) throws DatabaseException {
      try {
        entityConnection.beginTransaction();
        final EntitySelectCondition selectCondition = entitySelectCondition(T_INVOICE);
        selectCondition.setForUpdate(true);
        selectCondition.setForeignKeyFetchDepthLimit(0);
        final List<Entity> invoices = entityConnection.select(selectCondition);
        for (final Entity invoice : invoices) {
          invoice.put(INVOICE_TOTAL, invoice.get(INVOICE_TOTAL_SUB));
        }
        final List<Entity> modifiedInvoices = getModifiedEntities(invoices);
        if (!modifiedInvoices.isEmpty()) {
          entityConnection.update(modifiedInvoices);
        }
        entityConnection.commitTransaction();
      }
      catch (final DatabaseException exception) {
        entityConnection.rollbackTransaction();
        throw exception;
      }
    }
  }

  private static final class RaisePriceFunction extends AbstractFunction<LocalEntityConnection> {

    private RaisePriceFunction() {
      super(F_RAISE_PRICE, "Raise track prices");
    }

    @Override
    public List execute(final LocalEntityConnection entityConnection,
                        final Object... arguments) throws DatabaseException {
      final List<Long> trackIds = (List<Long>) arguments[0];
      final BigDecimal priceIncrease = (BigDecimal) arguments[1];
      try {
        entityConnection.beginTransaction();

        final EntitySelectCondition selectCondition = entitySelectCondition(T_TRACK,
                TRACK_TRACKID, ConditionType.LIKE, trackIds);
        selectCondition.setForUpdate(true);

        final List<Entity> tracks = entityConnection.select(selectCondition);
        tracks.forEach(track ->
                track.put(TRACK_UNITPRICE,
                        track.getBigDecimal(TRACK_UNITPRICE).add(priceIncrease)));
        final List<Entity> updatedTracks = entityConnection.update(tracks);

        entityConnection.commitTransaction();

        return updatedTracks;
      }
      catch (final DatabaseException exception) {
        entityConnection.rollbackTransaction();
        throw exception;
      }
    }
  }

  private static final class CoverArtImageProvider implements DerivedProperty.Provider {

    @Override
    public Object getValue(final Map<String, Object> sourceValues) {
      final byte[] bytes = (byte[]) sourceValues.get(ALBUM_COVERART);
      if (bytes == null) {
        return null;
      }

      try {
        return ImageIO.read(new ByteArrayInputStream(bytes));
      }
      catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}