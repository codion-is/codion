/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain.impl;

import is.codion.common.db.Operator;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.AbstractDatabaseFunction;
import is.codion.common.db.operation.AbstractDatabaseProcedure;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.DerivedProperty;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static is.codion.framework.db.condition.Conditions.selectCondition;
import static is.codion.framework.domain.entity.Entities.getModifiedEntities;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;

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
    addReport(CUSTOMER_REPORT);
    addOperation(new UpdateTotalsProcedure());
    addOperation(new RaisePriceFunction());
  }

  void artist() {
    define(T_ARTIST, "chinook.artist",
            primaryKeyProperty(ARTIST_ARTISTID),
            columnProperty(ARTIST_NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160),
            subqueryProperty(ARTIST_NR_OF_ALBUMS, "Albums",
                    "select count(*) " +
                            "from chinook.album " +
                            "where album.artistid = artist.artistid"),
            subqueryProperty(ARTIST_NR_OF_TRACKS, "Tracks",
                    "select count(*) " +
                            "from chinook.track " +
                            "where track.albumid in (" +
                            "  select albumid " +
                            "  from chinook.album " +
                            "  where album.artistid = artist.artistid" +
                            ")"))
            .keyGenerator(automatic("chinook.artist"))
            .orderBy(orderBy().ascending(ARTIST_NAME))
            .stringProvider(new StringProvider(ARTIST_NAME))
            .caption("Artists");
  }

  void album() {
    define(T_ALBUM, "chinook.album",
            primaryKeyProperty(ALBUM_ALBUMID),
            foreignKeyProperty(ALBUM_ARTIST_FK, "Artist", T_ARTIST,
                    columnProperty(ALBUM_ARTISTID))
                    .nullable(false)
                    .preferredColumnWidth(160),
            columnProperty(ALBUM_TITLE, "Title")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(160)
                    .preferredColumnWidth(160),
            blobProperty(ALBUM_COVER, "Cover")
                    .eagerlyLoaded(true),
            derivedProperty(ALBUM_COVER_IMAGE, null,
                    new CoverArtImageProvider(), ALBUM_COVER),
            subqueryProperty(ALBUM_NUMBER_OF_TRACKS, "Tracks",
                    "select count(*) " +
                            "from chinook.track " +
                            "where track.albumid = album.albumid"))
            .keyGenerator(automatic("chinook.album"))
            .orderBy(orderBy().ascending(ALBUM_ARTISTID, ALBUM_TITLE))
            .stringProvider(new StringProvider(ALBUM_TITLE))
            .caption("Albums");
  }

  void employee() {
    define(T_EMPLOYEE, "chinook.employee",
            primaryKeyProperty(EMPLOYEE_EMPLOYEEID),
            columnProperty(EMPLOYEE_LASTNAME, "Last name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(EMPLOYEE_FIRSTNAME, "First name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(EMPLOYEE_TITLE, "Title")
                    .maximumLength(30),
            foreignKeyProperty(EMPLOYEE_REPORTSTO_FK, "Reports to", T_EMPLOYEE,
                    columnProperty(EMPLOYEE_REPORTSTO)),
            columnProperty(EMPLOYEE_BIRTHDATE, "Birthdate"),
            columnProperty(EMPLOYEE_HIREDATE, "Hiredate"),
            columnProperty(EMPLOYEE_ADDRESS, "Address")
                    .maximumLength(70),
            columnProperty(EMPLOYEE_CITY, "City")
                    .maximumLength(40),
            columnProperty(EMPLOYEE_STATE, "State")
                    .maximumLength(40),
            columnProperty(EMPLOYEE_COUNTRY, "Country")
                    .maximumLength(40),
            columnProperty(EMPLOYEE_POSTALCODE, "Postal code")
                    .maximumLength(10),
            columnProperty(EMPLOYEE_PHONE, "Phone")
                    .maximumLength(24),
            columnProperty(EMPLOYEE_FAX, "Fax")
                    .maximumLength(24),
            columnProperty(EMPLOYEE_EMAIL, "Email")
                    .searchProperty(true)
                    .maximumLength(60))
            .keyGenerator(automatic("chinook.employee"))
            .orderBy(orderBy().ascending(EMPLOYEE_LASTNAME, EMPLOYEE_FIRSTNAME))
            .stringProvider(new StringProvider(EMPLOYEE_LASTNAME)
                    .addText(", ").addValue(EMPLOYEE_FIRSTNAME))
            .caption("Employees");
  }

  void customer() {
    define(T_CUSTOMER, "chinook.customer",
            primaryKeyProperty(CUSTOMER_CUSTOMERID),
            columnProperty(CUSTOMER_LASTNAME, "Last name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(CUSTOMER_FIRSTNAME, "First name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(CUSTOMER_COMPANY, "Company")
                    .maximumLength(80),
            columnProperty(CUSTOMER_ADDRESS, "Address")
                    .maximumLength(70),
            columnProperty(CUSTOMER_CITY, "City")
                    .maximumLength(40),
            columnProperty(CUSTOMER_STATE, "State")
                    .maximumLength(40),
            columnProperty(CUSTOMER_COUNTRY, "Country")
                    .maximumLength(40),
            columnProperty(CUSTOMER_POSTALCODE, "Postal code")
                    .maximumLength(10),
            columnProperty(CUSTOMER_PHONE, "Phone")
                    .maximumLength(24),
            columnProperty(CUSTOMER_FAX, "Fax")
                    .maximumLength(24),
            columnProperty(CUSTOMER_EMAIL, "Email")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(60),
            foreignKeyProperty(CUSTOMER_SUPPORTREP_FK, "Support rep", T_EMPLOYEE,
                    columnProperty(CUSTOMER_SUPPORTREPID)))
            .keyGenerator(automatic("chinook.customer"))
            .orderBy(orderBy().ascending(CUSTOMER_LASTNAME, CUSTOMER_FIRSTNAME))
            .stringProvider(new CustomerStringProvider())
            .caption("Customers");
  }

  void genre() {
    define(T_GENRE, "chinook.genre",
            primaryKeyProperty(GENRE_GENREID),
            columnProperty(GENRE_NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.genre"))
            .orderBy(orderBy().ascending(GENRE_NAME))
            .stringProvider(new StringProvider(GENRE_NAME))
            .smallDataset(true)
            .caption("Genres");
  }

  void mediaType() {
    define(T_MEDIATYPE, "chinook.mediatype",
            primaryKeyProperty(MEDIATYPE_MEDIATYPEID),
            columnProperty(MEDIATYPE_NAME, "Name")
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
            primaryKeyProperty(TRACK_TRACKID),
            denormalizedViewProperty(TRACK_ARTIST_DENORM, TRACK_ALBUM_FK, ALBUM_ARTIST_FK, "Artist")
                    .preferredColumnWidth(160),
            // tag::fetchDepth2[]
            foreignKeyProperty(TRACK_ALBUM_FK, "Album", T_ALBUM,
                    columnProperty(TRACK_ALBUMID))
                    .fetchDepth(2)
                    .preferredColumnWidth(160),
            // end::fetchDepth2[]
            columnProperty(TRACK_NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(200)
                    .preferredColumnWidth(160),
            foreignKeyProperty(TRACK_GENRE_FK, "Genre", T_GENRE,
                    columnProperty(TRACK_GENREID)),
            columnProperty(TRACK_COMPOSER, "Composer")
                    .maximumLength(220)
                    .preferredColumnWidth(160),
            foreignKeyProperty(TRACK_MEDIATYPE_FK, "Media type", T_MEDIATYPE,
                    columnProperty(TRACK_MEDIATYPEID))
                    .nullable(false),
            columnProperty(TRACK_MILLISECONDS, "Duration (ms)")
                    .nullable(false)
                    .format(NumberFormat.getIntegerInstance()),
            derivedProperty(TRACK_MINUTES_SECONDS_DERIVED, "Duration (min/sec)",
                    TRACK_MIN_SEC_PROVIDER, TRACK_MILLISECONDS),
            columnProperty(TRACK_BYTES, "Bytes")
                    .format(NumberFormat.getIntegerInstance()),
            columnProperty(TRACK_UNITPRICE, "Price")
                    .nullable(false)
                    .maximumFractionDigits(2))
            .keyGenerator(automatic("chinook.track"))
            .orderBy(orderBy().ascending(TRACK_NAME))
            .stringProvider(new StringProvider(TRACK_NAME))
            .caption("Tracks");
  }

  void invoice() {
    define(T_INVOICE, "chinook.invoice",
            primaryKeyProperty(INVOICE_INVOICEID, "Invoice no."),
            foreignKeyProperty(INVOICE_CUSTOMER_FK, "Customer", T_CUSTOMER,
                    columnProperty(INVOICE_CUSTOMERID))
                    .nullable(false),
            columnProperty(INVOICE_INVOICEDATE, "Date/time")
                    .nullable(false),
            columnProperty(INVOICE_BILLINGADDRESS, "Billing address")
                    .maximumLength(70),
            columnProperty(INVOICE_BILLINGCITY, "Billing city")
                    .maximumLength(40),
            columnProperty(INVOICE_BILLINGSTATE, "Billing state")
                    .maximumLength(40),
            columnProperty(INVOICE_BILLINGCOUNTRY, "Billing country")
                    .maximumLength(40),
            columnProperty(INVOICE_BILLINGPOSTALCODE, "Billing postal code")
                    .maximumLength(10),
            columnProperty(INVOICE_TOTAL, "Total")
                    .maximumFractionDigits(2)
                    .hidden(true),
            subqueryProperty(INVOICE_TOTAL_SUB, "Calculated total",
                    "select sum(unitprice * quantity) from chinook.invoiceline " +
                            "where invoiceid = invoice.invoiceid")
                    .maximumFractionDigits(2))
            .keyGenerator(automatic("chinook.invoice"))
            .orderBy(orderBy().ascending(INVOICE_CUSTOMERID).descending(INVOICE_INVOICEDATE))
            .stringProvider(new StringProvider(INVOICE_INVOICEID))
            .caption("Invoices");
  }

  void invoiceLine() {
    define(T_INVOICELINE, "chinook.invoiceline",
            primaryKeyProperty(INVOICELINE_INVOICELINEID),
            // tag::fetchDepth0[]
            foreignKeyProperty(INVOICELINE_INVOICE_FK, "Invoice", T_INVOICE,
                    columnProperty(INVOICELINE_INVOICEID))
                    .fetchDepth(0)
                    .nullable(false),
            // end::fetchDepth0[]
            foreignKeyProperty(INVOICELINE_TRACK_FK, "Track", T_TRACK,
                    columnProperty(INVOICELINE_TRACKID))
                    .nullable(false)
                    .preferredColumnWidth(100),
            denormalizedProperty(INVOICELINE_UNITPRICE, INVOICELINE_TRACK_FK, TRACK_UNITPRICE, "Unit price")
                    .nullable(false),
            columnProperty(INVOICELINE_QUANTITY, "Quantity")
                    .nullable(false)
                    .defaultValue(1),
            derivedProperty(INVOICELINE_TOTAL, "Total", INVOICELINE_TOTAL_PROVIDER,
                    INVOICELINE_QUANTITY, INVOICELINE_UNITPRICE))
            .keyGenerator(automatic("chinook.invoiceline"))
            .caption("Invoice lines");
  }

  void playlist() {
    define(T_PLAYLIST, "chinook.playlist",
            primaryKeyProperty(PLAYLIST_PLAYLISTID),
            columnProperty(PLAYLIST_NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.playlist"))
            .orderBy(orderBy().ascending(PLAYLIST_NAME))
            .stringProvider(new StringProvider(PLAYLIST_NAME))
            .caption("Playlists");
  }

  void playlistTrack() {
    define(T_PLAYLISTTRACK, "chinook.playlisttrack",
            primaryKeyProperty(PLAYLISTTRACK_ID),
            foreignKeyProperty(PLAYLISTTRACK_PLAYLIST_FK, "Playlist", T_PLAYLIST,
                    columnProperty(PLAYLISTTRACK_PLAYLISTID))
                    .nullable(false)
                    .preferredColumnWidth(120),
            denormalizedViewProperty(PLAYLISTTRACK_ARTIST_DENORM, PLAYLISTTRACK_ALBUM_DENORM, ALBUM_ARTIST_FK, "Artist")
                    .preferredColumnWidth(160),
            foreignKeyProperty(PLAYLISTTRACK_TRACK_FK, "Track", T_TRACK,
                    columnProperty(PLAYLISTTRACK_TRACKID))
                    .fetchDepth(3)
                    .nullable(false)
                    .preferredColumnWidth(160),
            denormalizedViewProperty(PLAYLISTTRACK_ALBUM_DENORM, PLAYLISTTRACK_TRACK_FK, TRACK_ALBUM_FK, "Album")
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.playlisttrack"))
            .stringProvider(new StringProvider(PLAYLISTTRACK_PLAYLIST_FK)
                    .addText(" - ").addValue(PLAYLISTTRACK_TRACK_FK))
            .caption("Playlist tracks");
  }

  private static final class UpdateTotalsProcedure
          extends AbstractDatabaseProcedure<LocalEntityConnection> {

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
        selectCondition.setForeignKeyFetchDepth(0);
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

  private static final class RaisePriceFunction
          extends AbstractDatabaseFunction<LocalEntityConnection, List<Entity>> {

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
                        track.get(TRACK_UNITPRICE).add(priceIncrease)));
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

  private static final class CoverArtImageProvider
          implements DerivedProperty.Provider<Image> {

    private static final long serialVersionUID = 1;

    @Override
    public Image getValue(final Map<Attribute<?>, Object> sourceValues) {
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

  private static final class CustomerStringProvider
          implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public String apply(final Entity customer) {
      final StringBuilder builder = new StringBuilder();
      if (customer.isNotNull(CUSTOMER_LASTNAME)) {
        builder.append(customer.get(CUSTOMER_LASTNAME));
      }
      if (customer.isNotNull(CUSTOMER_FIRSTNAME)) {
        builder.append(", ").append(customer.get(CUSTOMER_FIRSTNAME));
      }
      if (customer.isNotNull(CUSTOMER_EMAIL)) {
        builder.append(" <").append(customer.get(CUSTOMER_EMAIL)).append(">");
      }

      return builder.toString();
    }
  }
}