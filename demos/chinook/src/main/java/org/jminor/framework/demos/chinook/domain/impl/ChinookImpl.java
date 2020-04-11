/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.domain.impl;

import org.jminor.common.db.Operator;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.operation.AbstractDatabaseFunction;
import org.jminor.common.db.operation.AbstractDatabaseProcedure;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.StringProvider;
import org.jminor.framework.domain.property.DerivedProperty;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.jminor.framework.db.condition.Conditions.selectCondition;
import static org.jminor.framework.domain.entity.Entities.getModifiedEntities;
import static org.jminor.framework.domain.entity.KeyGenerators.automatic;
import static org.jminor.framework.domain.entity.OrderBy.orderBy;
import static org.jminor.framework.domain.property.Properties.*;

public final class ChinookImpl extends Domain implements Chinook {

  public ChinookImpl() {
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
            primaryKeyProperty(ARTIST_ARTISTID, Types.BIGINT),
            columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.artist"))
            .orderBy(orderBy().ascending(ARTIST_NAME))
            .stringProvider(new StringProvider(ARTIST_NAME))
            .searchPropertyIds(ARTIST_NAME)
            .caption("Artists");
  }

  void album() {
    define(T_ALBUM, "chinook.album",
            primaryKeyProperty(ALBUM_ALBUMID, Types.BIGINT),
            foreignKeyProperty(ALBUM_ARTIST_FK, "Artist", T_ARTIST,
                    columnProperty(ALBUM_ARTISTID, Types.BIGINT))
                    .nullable(false)
                    .preferredColumnWidth(160),
            columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                    .nullable(false)
                    .maximumLength(160)
                    .preferredColumnWidth(160),
            blobProperty(ALBUM_COVER, "Cover")
                    .eagerlyLoaded(true),
            derivedProperty(ALBUM_COVER_IMAGE, Types.JAVA_OBJECT, null,
                    new CoverArtImageProvider(), ALBUM_COVER))
            .keyGenerator(automatic("chinook.album"))
            .orderBy(orderBy().ascending(ALBUM_ARTISTID, ALBUM_TITLE))
            .stringProvider(new StringProvider(ALBUM_TITLE))
            .searchPropertyIds(ALBUM_TITLE)
            .caption("Albums");
  }

  void employee() {
    define(T_EMPLOYEE, "chinook.employee",
            primaryKeyProperty(EMPLOYEE_EMPLOYEEID, Types.BIGINT),
            columnProperty(EMPLOYEE_LASTNAME, Types.VARCHAR, "Last name")
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(EMPLOYEE_FIRSTNAME, Types.VARCHAR, "First name")
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(EMPLOYEE_TITLE, Types.VARCHAR, "Title")
                    .maximumLength(30),
            foreignKeyProperty(EMPLOYEE_REPORTSTO_FK, "Reports to", T_EMPLOYEE,
                    columnProperty(EMPLOYEE_REPORTSTO, Types.BIGINT)),
            columnProperty(EMPLOYEE_BIRTHDATE, Types.DATE, "Birthdate"),
            columnProperty(EMPLOYEE_HIREDATE, Types.DATE, "Hiredate"),
            columnProperty(EMPLOYEE_ADDRESS, Types.VARCHAR, "Address")
                    .maximumLength(70),
            columnProperty(EMPLOYEE_CITY, Types.VARCHAR, "City")
                    .maximumLength(40),
            columnProperty(EMPLOYEE_STATE, Types.VARCHAR, "State")
                    .maximumLength(40),
            columnProperty(EMPLOYEE_COUNTRY, Types.VARCHAR, "Country")
                    .maximumLength(40),
            columnProperty(EMPLOYEE_POSTALCODE, Types.VARCHAR, "Postal code")
                    .maximumLength(10),
            columnProperty(EMPLOYEE_PHONE, Types.VARCHAR, "Phone")
                    .maximumLength(24),
            columnProperty(EMPLOYEE_FAX, Types.VARCHAR, "Fax")
                    .maximumLength(24),
            columnProperty(EMPLOYEE_EMAIL, Types.VARCHAR, "Email")
                    .maximumLength(60))
            .keyGenerator(automatic("chinook.employee"))
            .orderBy(orderBy().ascending(EMPLOYEE_LASTNAME, EMPLOYEE_FIRSTNAME))
            .stringProvider(new StringProvider(EMPLOYEE_LASTNAME)
                    .addText(", ").addValue(EMPLOYEE_FIRSTNAME))
            .searchPropertyIds(EMPLOYEE_FIRSTNAME, EMPLOYEE_LASTNAME, EMPLOYEE_EMAIL)
            .caption("Employees");
  }

  void customer() {
    define(T_CUSTOMER, "chinook.customer",
            primaryKeyProperty(CUSTOMER_CUSTOMERID, Types.BIGINT),
            columnProperty(CUSTOMER_LASTNAME, Types.VARCHAR, "Last name")
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(CUSTOMER_FIRSTNAME, Types.VARCHAR, "First name")
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(CUSTOMER_COMPANY, Types.VARCHAR, "Company")
                    .maximumLength(80),
            columnProperty(CUSTOMER_ADDRESS, Types.VARCHAR, "Address")
                    .maximumLength(70),
            columnProperty(CUSTOMER_CITY, Types.VARCHAR, "City")
                    .maximumLength(40),
            columnProperty(CUSTOMER_STATE, Types.VARCHAR, "State")
                    .maximumLength(40),
            columnProperty(CUSTOMER_COUNTRY, Types.VARCHAR, "Country")
                    .maximumLength(40),
            columnProperty(CUSTOMER_POSTALCODE, Types.VARCHAR, "Postal code")
                    .maximumLength(10),
            columnProperty(CUSTOMER_PHONE, Types.VARCHAR, "Phone")
                    .maximumLength(24),
            columnProperty(CUSTOMER_FAX, Types.VARCHAR, "Fax")
                    .maximumLength(24),
            columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email")
                    .nullable(false)
                    .maximumLength(60),
            foreignKeyProperty(CUSTOMER_SUPPORTREP_FK, "Support rep", T_EMPLOYEE,
                    columnProperty(CUSTOMER_SUPPORTREPID, Types.BIGINT)))
            .keyGenerator(automatic("chinook.customer"))
            .orderBy(orderBy().ascending(CUSTOMER_LASTNAME, CUSTOMER_FIRSTNAME))
            .stringProvider(new CustomerStringProvider())
            .searchPropertyIds(CUSTOMER_FIRSTNAME, CUSTOMER_LASTNAME, CUSTOMER_EMAIL)
            .caption("Customers");
  }

  void genre() {
    define(T_GENRE, "chinook.genre",
            primaryKeyProperty(GENRE_GENREID, Types.BIGINT),
            columnProperty(GENRE_NAME, Types.VARCHAR, "Name")
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.genre"))
            .orderBy(orderBy().ascending(GENRE_NAME))
            .stringProvider(new StringProvider(GENRE_NAME))
            .searchPropertyIds(GENRE_NAME)
            .smallDataset(true)
            .caption("Genres");
  }

  void mediaType() {
    define(T_MEDIATYPE, "chinook.mediatype",
            primaryKeyProperty(MEDIATYPE_MEDIATYPEID, Types.BIGINT),
            columnProperty(MEDIATYPE_NAME, Types.VARCHAR, "Name")
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.mediatype"))
            .stringProvider(new StringProvider(MEDIATYPE_NAME))
            .smallDataset(true)
            .caption("Media types");
  }

  void track() {
    define(T_TRACK, "chinook.track",
            primaryKeyProperty(TRACK_TRACKID, Types.BIGINT),
            denormalizedViewProperty(TRACK_ARTIST_DENORM, TRACK_ALBUM_FK,
                    getDefinition(T_ALBUM).getProperty(ALBUM_ARTIST_FK), "Artist")
                    .preferredColumnWidth(160),
            foreignKeyProperty(TRACK_ALBUM_FK, "Album", T_ALBUM,
                    columnProperty(TRACK_ALBUMID, Types.BIGINT))
                    .fetchDepth(2)
                    .preferredColumnWidth(160),
            columnProperty(TRACK_NAME, Types.VARCHAR, "Name")
                    .nullable(false)
                    .maximumLength(200)
                    .preferredColumnWidth(160),
            foreignKeyProperty(TRACK_GENRE_FK, "Genre", T_GENRE,
                    columnProperty(TRACK_GENREID, Types.BIGINT)),
            columnProperty(TRACK_COMPOSER, Types.VARCHAR, "Composer")
                    .maximumLength(220)
                    .preferredColumnWidth(160),
            foreignKeyProperty(TRACK_MEDIATYPE_FK, "Media type", T_MEDIATYPE,
                    columnProperty(TRACK_MEDIATYPEID, Types.BIGINT))
                    .nullable(false),
            columnProperty(TRACK_MILLISECONDS, Types.INTEGER, "Duration (ms)")
                    .nullable(false)
                    .format(NumberFormat.getIntegerInstance()),
            derivedProperty(TRACK_MINUTES_SECONDS_DERIVED, Types.VARCHAR, "Duration (min/sec)",
                    TRACK_MIN_SEC_PROVIDER, TRACK_MILLISECONDS),
            columnProperty(TRACK_BYTES, Types.INTEGER, "Bytes")
                    .format(NumberFormat.getIntegerInstance()),
            columnProperty(TRACK_UNITPRICE, Types.DECIMAL, "Price")
                    .nullable(false)
                    .maximumFractionDigits(2))
            .keyGenerator(automatic("chinook.track"))
            .orderBy(orderBy().ascending(TRACK_NAME))
            .stringProvider(new StringProvider(TRACK_NAME))
            .searchPropertyIds(TRACK_NAME)
            .caption("Tracks");
  }

  void invoice() {
    define(T_INVOICE, "chinook.invoice",
            primaryKeyProperty(INVOICE_INVOICEID, Types.BIGINT, "Invoice no."),
            columnProperty(INVOICE_INVOICEID_AS_STRING, Types.VARCHAR, "Invoice no.")
                    .readOnly(true)
                    .hidden(true),
            foreignKeyProperty(INVOICE_CUSTOMER_FK, "Customer", T_CUSTOMER,
                    columnProperty(INVOICE_CUSTOMERID, Types.BIGINT))
                    .nullable(false),
            columnProperty(INVOICE_INVOICEDATE, Types.TIMESTAMP, "Date/time")
                    .nullable(false),
            columnProperty(INVOICE_BILLINGADDRESS, Types.VARCHAR, "Billing address")
                    .maximumLength(70),
            columnProperty(INVOICE_BILLINGCITY, Types.VARCHAR, "Billing city")
                    .maximumLength(40),
            columnProperty(INVOICE_BILLINGSTATE, Types.VARCHAR, "Billing state")
                    .maximumLength(40),
            columnProperty(INVOICE_BILLINGCOUNTRY, Types.VARCHAR, "Billing country")
                    .maximumLength(40),
            columnProperty(INVOICE_BILLINGPOSTALCODE, Types.VARCHAR, "Billing postal code")
                    .maximumLength(10),
            columnProperty(INVOICE_TOTAL, Types.DECIMAL, "Total")
                    .maximumFractionDigits(2)
                    .hidden(true),
            subqueryProperty(INVOICE_TOTAL_SUB, Types.DECIMAL, "Calculated total",
                    "select sum(unitprice * quantity) from chinook.invoiceline " +
                            "where invoiceid = invoice.invoiceid")
                    .maximumFractionDigits(2))
            .keyGenerator(automatic("chinook.invoice"))
            .orderBy(orderBy().ascending(INVOICE_CUSTOMERID).descending(INVOICE_INVOICEDATE))
            .stringProvider(new StringProvider(INVOICE_INVOICEID))
            .searchPropertyIds(INVOICE_INVOICEID_AS_STRING)
            .caption("Invoices");
  }

  void invoiceLine() {
    define(T_INVOICELINE, "chinook.invoiceline",
            primaryKeyProperty(INVOICELINE_INVOICELINEID, Types.BIGINT),
            foreignKeyProperty(INVOICELINE_INVOICE_FK, "Invoice", T_INVOICE,
                    columnProperty(INVOICELINE_INVOICEID, Types.BIGINT))
                    .fetchDepth(0)
                    .nullable(false),
            foreignKeyProperty(INVOICELINE_TRACK_FK, "Track", T_TRACK,
                    columnProperty(INVOICELINE_TRACKID, Types.BIGINT))
                    .nullable(false)
                    .preferredColumnWidth(100),
            denormalizedProperty(INVOICELINE_UNITPRICE, INVOICELINE_TRACK_FK,
                    getDefinition(T_TRACK).getProperty(TRACK_UNITPRICE), "Unit price")
                    .nullable(false),
            columnProperty(INVOICELINE_QUANTITY, Types.INTEGER, "Quantity")
                    .nullable(false),
            derivedProperty(INVOICELINE_TOTAL, Types.DOUBLE, "Total", INVOICELINE_TOTAL_PROVIDER,
                    INVOICELINE_QUANTITY, INVOICELINE_UNITPRICE))
            .keyGenerator(automatic("chinook.invoiceline"))
            .caption("Invoice lines");
  }

  void playlist() {
    define(T_PLAYLIST, "chinook.playlist",
            primaryKeyProperty(PLAYLIST_PLAYLISTID, Types.BIGINT),
            columnProperty(PLAYLIST_NAME, Types.VARCHAR, "Name")
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.playlist"))
            .orderBy(orderBy().ascending(PLAYLIST_NAME))
            .stringProvider(new StringProvider(PLAYLIST_NAME))
            .searchPropertyIds(PLAYLIST_NAME)
            .caption("Playlists");
  }

  void playlistTrack() {
    define(T_PLAYLISTTRACK, "chinook.playlisttrack",
            foreignKeyProperty(PLAYLISTTRACK_PLAYLIST_FK, "Playlist", T_PLAYLIST,
                    primaryKeyProperty(PLAYLISTTRACK_PLAYLISTID, Types.BIGINT)
                            .updatable(true))
                    .nullable(false)
                    .preferredColumnWidth(120),
            denormalizedViewProperty(PLAYLISTTRACK_ARTIST_DENORM, PLAYLISTTRACK_ALBUM_DENORM,
                    getDefinition(T_ALBUM).getProperty(ALBUM_ARTIST_FK), "Artist")
                    .preferredColumnWidth(160),
            foreignKeyProperty(PLAYLISTTRACK_TRACK_FK, "Track", T_TRACK,
                    primaryKeyProperty(PLAYLISTTRACK_TRACKID, Types.BIGINT)
                            .primaryKeyIndex(1)
                            .updatable(true))
                    .fetchDepth(3)
                    .nullable(false)
                    .preferredColumnWidth(160),
            denormalizedViewProperty(PLAYLISTTRACK_ALBUM_DENORM, PLAYLISTTRACK_TRACK_FK,
                    getDefinition(T_TRACK).getProperty(TRACK_ALBUM_FK), "Album")
                    .preferredColumnWidth(160))
            .stringProvider(new StringProvider(PLAYLISTTRACK_PLAYLIST_FK)
                    .addText(" - ").addValue(PLAYLISTTRACK_TRACK_FK))
            .caption("Playlist tracks");
  }

  void dbOperations() {
    addOperation(new UpdateTotalsProcedure());
    addOperation(new RaisePriceFunction());
  }

  private static final class UpdateTotalsProcedure extends AbstractDatabaseProcedure<LocalEntityConnection> {

    private UpdateTotalsProcedure() {
      super(P_UPDATE_TOTALS, "Update invoice totals");
    }

    @Override
    public void execute(final LocalEntityConnection entityConnection,
                        final Object... arguments) throws DatabaseException {
      try {
        entityConnection.beginTransaction();
        final EntitySelectCondition selectCondition = selectCondition(T_INVOICE);
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

  private static final class RaisePriceFunction extends AbstractDatabaseFunction<LocalEntityConnection, List<Entity>> {

    private RaisePriceFunction() {
      super(F_RAISE_PRICE, "Raise track prices");
    }

    @Override
    public List<Entity> execute(final LocalEntityConnection entityConnection,
                        final Object... arguments) throws DatabaseException {
      final List<Long> trackIds = (List<Long>) arguments[0];
      final BigDecimal priceIncrease = (BigDecimal) arguments[1];
      try {
        entityConnection.beginTransaction();

        final EntitySelectCondition selectCondition = selectCondition(T_TRACK,
                TRACK_TRACKID, Operator.LIKE, trackIds);
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

    private static final long serialVersionUID = 1;

    @Override
    public Object getValue(final Map<String, Object> sourceValues) {
      final byte[] bytes = (byte[]) sourceValues.get(ALBUM_COVER);
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

  private static final class CustomerStringProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public String apply(final Entity customer) {
      final StringBuilder builder =
              new StringBuilder(customer.getString(CUSTOMER_LASTNAME))
                      .append(", ").append(customer.getString(CUSTOMER_FIRSTNAME));
      if (customer.isNotNull(CUSTOMER_EMAIL)) {
        builder.append(" <").append(customer.getString(CUSTOMER_EMAIL)).append(">");
      }

      return builder.toString();
    }
  }
}