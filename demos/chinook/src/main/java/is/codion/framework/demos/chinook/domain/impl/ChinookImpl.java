/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain.impl;

import is.codion.common.db.Operator;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.domain.DefaultDomain;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.framework.db.condition.Conditions.selectCondition;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;

public final class ChinookImpl extends DefaultDomain implements Chinook {

  public ChinookImpl() {
    super(DOMAIN);
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
    defineReport(Customer.REPORT, classPathReport(Chinook.class, "customer_report.jasper"));
    defineProcedure(Procedures.UPDATE_TOTALS, new UpdateTotalsProcedure());
    defineFunction(Functions.RAISE_PRICE, new RaisePriceFunction());
  }

  void artist() {
    define(Artist.TYPE, "chinook.artist",
            primaryKeyProperty(Artist.ID),
            columnProperty(Artist.NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160),
            subqueryProperty(Artist.NUMBER_OF_ALBUMS, "Albums",
                    "select count(*) " +
                            "from chinook.album " +
                            "where album.artistid = artist.artistid"),
            subqueryProperty(Artist.NUMBER_OF_TRACKS, "Tracks",
                    "select count(*) " +
                            "from chinook.track " +
                            "where track.albumid in (" +
                            "  select albumid " +
                            "  from chinook.album " +
                            "  where album.artistid = artist.artistid" +
                            ")"))
            .keyGenerator(automatic("chinook.artist"))
            .orderBy(orderBy().ascending(Artist.NAME))
            .stringProvider(new StringProvider(Artist.NAME))
            .caption("Artists");
  }

  void album() {
    define(Album.TYPE, "chinook.album",
            primaryKeyProperty(Album.ID),
            foreignKeyProperty(Album.ARTIST_FK, "Artist", Artist.TYPE,
                    columnProperty(Album.ARTIST_ID))
                    .nullable(false)
                    .preferredColumnWidth(160),
            columnProperty(Album.TITLE, "Title")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(160)
                    .preferredColumnWidth(160),
            blobProperty(Album.COVER, "Cover")
                    .eagerlyLoaded(true),
            derivedProperty(Album.COVERIMAGE, null,
                    new CoverArtImageProvider(), Album.COVER),
            subqueryProperty(Album.NUMBER_OF_TRACKS, "Tracks",
                    "select count(*) " +
                            "from chinook.track " +
                            "where track.albumid = album.albumid"))
            .keyGenerator(automatic("chinook.album"))
            .orderBy(orderBy().ascending(Album.ARTIST_ID, Album.TITLE))
            .stringProvider(new StringProvider(Album.TITLE))
            .caption("Albums");
  }

  void employee() {
    define(Employee.TYPE, "chinook.employee",
            primaryKeyProperty(Employee.ID),
            columnProperty(Employee.LASTNAME, "Last name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(Employee.FIRSTNAME, "First name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(Employee.TITLE, "Title")
                    .maximumLength(30),
            foreignKeyProperty(Employee.REPORTSTO_FK, "Reports to", Employee.TYPE,
                    columnProperty(Employee.REPORTSTO)),
            columnProperty(Employee.BIRTHDATE, "Birthdate"),
            columnProperty(Employee.HIREDATE, "Hiredate"),
            columnProperty(Employee.ADDRESS, "Address")
                    .maximumLength(70),
            columnProperty(Employee.CITY, "City")
                    .maximumLength(40),
            columnProperty(Employee.STATE, "State")
                    .maximumLength(40),
            columnProperty(Employee.COUNTRY, "Country")
                    .maximumLength(40),
            columnProperty(Employee.POSTALCODE, "Postal code")
                    .maximumLength(10),
            columnProperty(Employee.PHONE, "Phone")
                    .maximumLength(24),
            columnProperty(Employee.FAX, "Fax")
                    .maximumLength(24),
            columnProperty(Employee.EMAIL, "Email")
                    .searchProperty(true)
                    .maximumLength(60))
            .keyGenerator(automatic("chinook.employee"))
            .orderBy(orderBy().ascending(Employee.LASTNAME, Employee.FIRSTNAME))
            .stringProvider(new StringProvider(Employee.LASTNAME)
                    .addText(", ").addValue(Employee.FIRSTNAME))
            .caption("Employees");
  }

  void customer() {
    define(Customer.TYPE, "chinook.customer",
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.LASTNAME, "Last name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(Customer.FIRSTNAME, "First name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.COMPANY, "Company")
                    .maximumLength(80),
            columnProperty(Customer.ADDRESS, "Address")
                    .maximumLength(70),
            columnProperty(Customer.CITY, "City")
                    .maximumLength(40),
            columnProperty(Customer.STATE, "State")
                    .maximumLength(40),
            columnProperty(Customer.COUNTRY, "Country")
                    .maximumLength(40),
            columnProperty(Customer.POSTALCODE, "Postal code")
                    .maximumLength(10),
            columnProperty(Customer.PHONE, "Phone")
                    .maximumLength(24),
            columnProperty(Customer.FAX, "Fax")
                    .maximumLength(24),
            columnProperty(Customer.EMAIL, "Email")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(60),
            foreignKeyProperty(Customer.SUPPORTREP_FK, "Support rep", Employee.TYPE,
                    columnProperty(Customer.SUPPORTREP_ID)))
            .keyGenerator(automatic("chinook.customer"))
            .orderBy(orderBy().ascending(Customer.LASTNAME, Customer.FIRSTNAME))
            .stringProvider(new CustomerStringProvider())
            .caption("Customers");
  }

  void genre() {
    define(Genre.TYPE, "chinook.genre",
            primaryKeyProperty(Genre.ID),
            columnProperty(Genre.NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.genre"))
            .orderBy(orderBy().ascending(Genre.NAME))
            .stringProvider(new StringProvider(Genre.NAME))
            .smallDataset(true)
            .caption("Genres");
  }

  void mediaType() {
    define(MediaType.TYPE, "chinook.mediatype",
            primaryKeyProperty(MediaType.ID),
            columnProperty(MediaType.NAME, "Name")
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.mediatype"))
            .stringProvider(new StringProvider(MediaType.NAME))
            .smallDataset(true)
            .caption("Media types");
  }

  void track() {
    define(Track.TYPE, "chinook.track",
            primaryKeyProperty(Track.ID),
            denormalizedViewProperty(Track.ARTIST_DENORM, Track.ALBUM_FK, Album.ARTIST_FK, "Artist")
                    .preferredColumnWidth(160),
            // tag::fetchDepth2[]
            foreignKeyProperty(Track.ALBUM_FK, "Album", Album.TYPE,
                    columnProperty(Track.ALBUM_ID))
                    .fetchDepth(2)
                    .preferredColumnWidth(160),
            // end::fetchDepth2[]
            columnProperty(Track.NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(200)
                    .preferredColumnWidth(160),
            foreignKeyProperty(Track.GENRE_FK, "Genre", Genre.TYPE,
                    columnProperty(Track.GENRE_ID)),
            columnProperty(Track.COMPOSER, "Composer")
                    .maximumLength(220)
                    .preferredColumnWidth(160),
            foreignKeyProperty(Track.MEDIATYPE_FK, "Media type", MediaType.TYPE,
                    columnProperty(Track.MEDIATYPE_ID))
                    .nullable(false),
            columnProperty(Track.MILLISECONDS, "Duration (ms)")
                    .nullable(false)
                    .format(NumberFormat.getIntegerInstance()),
            derivedProperty(Track.MINUTES_SECONDS_DERIVED, "Duration (min/sec)",
                    new TrackMinSecProvider(), Track.MILLISECONDS),
            columnProperty(Track.BYTES, "Bytes")
                    .format(NumberFormat.getIntegerInstance()),
            columnProperty(Track.UNITPRICE, "Price")
                    .nullable(false)
                    .maximumFractionDigits(2)
                    .beanProperty("unitPrice"))
            .keyGenerator(automatic("chinook.track"))
            .orderBy(orderBy().ascending(Track.NAME))
            .stringProvider(new StringProvider(Track.NAME))
            .caption("Tracks");
  }

  void invoice() {
    define(Invoice.TYPE, "chinook.invoice",
            primaryKeyProperty(Invoice.ID, "Invoice no."),
            foreignKeyProperty(Invoice.CUSTOMER_FK, "Customer", Customer.TYPE,
                    columnProperty(Invoice.CUSTOMER_ID))
                    .nullable(false),
            columnProperty(Invoice.INVOICEDATE, "Date/time")
                    .nullable(false),
            columnProperty(Invoice.BILLINGADDRESS, "Billing address")
                    .maximumLength(70),
            columnProperty(Invoice.BILLINGCITY, "Billing city")
                    .maximumLength(40),
            columnProperty(Invoice.BILLINGSTATE, "Billing state")
                    .maximumLength(40),
            columnProperty(Invoice.BILLINGCOUNTRY, "Billing country")
                    .maximumLength(40),
            columnProperty(Invoice.BILLINGPOSTALCODE, "Billing postal code")
                    .maximumLength(10),
            columnProperty(Invoice.TOTAL, "Total")
                    .maximumFractionDigits(2)
                    .hidden(true),
            subqueryProperty(Invoice.TOTAL_SUBQUERY, "Calculated total",
                    "select sum(unitprice * quantity) from chinook.invoiceline " +
                            "where invoiceid = invoice.invoiceid")
                    .maximumFractionDigits(2))
            .keyGenerator(automatic("chinook.invoice"))
            .orderBy(orderBy().ascending(Invoice.CUSTOMER_ID).descending(Invoice.INVOICEDATE))
            .stringProvider(new StringProvider(Invoice.ID))
            .caption("Invoices");
  }

  void invoiceLine() {
    define(InvoiceLine.TYPE, "chinook.invoiceline",
            primaryKeyProperty(InvoiceLine.ID),
            // tag::fetchDepth0[]
            foreignKeyProperty(InvoiceLine.INVOICE_FK, "Invoice", Invoice.TYPE,
                    columnProperty(InvoiceLine.INVOICE_ID))
                    .fetchDepth(0)
                    .nullable(false),
            // end::fetchDepth0[]
            foreignKeyProperty(InvoiceLine.TRACK_FK, "Track", Track.TYPE,
                    columnProperty(InvoiceLine.TRACK_ID))
                    .nullable(false)
                    .preferredColumnWidth(100),
            denormalizedProperty(InvoiceLine.UNITPRICE, InvoiceLine.TRACK_FK, Track.UNITPRICE, "Unit price")
                    .nullable(false),
            columnProperty(InvoiceLine.QUANTITY, "Quantity")
                    .nullable(false)
                    .defaultValue(1),
            derivedProperty(InvoiceLine.TOTAL, "Total", new InvoiceLineTotalProvider(),
                    InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE))
            .keyGenerator(automatic("chinook.invoiceline"))
            .caption("Invoice lines");
  }

  void playlist() {
    define(Playlist.TYPE, "chinook.playlist",
            primaryKeyProperty(Playlist.ID),
            columnProperty(Playlist.NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.playlist"))
            .orderBy(orderBy().ascending(Playlist.NAME))
            .stringProvider(new StringProvider(Playlist.NAME))
            .caption("Playlists");
  }

  void playlistTrack() {
    define(PlaylistTrack.TYPE, "chinook.playlisttrack",
            primaryKeyProperty(PlaylistTrack.ID),
            foreignKeyProperty(PlaylistTrack.PLAYLIST_FK, "Playlist", Playlist.TYPE,
                    columnProperty(PlaylistTrack.PLAYLIST_ID))
                    .nullable(false)
                    .preferredColumnWidth(120),
            denormalizedViewProperty(PlaylistTrack.ARTIST_DENORM, PlaylistTrack.ALBUM_DENORM, Album.ARTIST_FK, "Artist")
                    .preferredColumnWidth(160),
            foreignKeyProperty(PlaylistTrack.TRACK_FK, "Track", Track.TYPE,
                    columnProperty(PlaylistTrack.TRACK_ID))
                    .fetchDepth(3)
                    .nullable(false)
                    .preferredColumnWidth(160),
            denormalizedViewProperty(PlaylistTrack.ALBUM_DENORM, PlaylistTrack.TRACK_FK, Track.ALBUM_FK, "Album")
                    .preferredColumnWidth(160))
            .keyGenerator(automatic("chinook.playlisttrack"))
            .stringProvider(new StringProvider(PlaylistTrack.PLAYLIST_FK)
                    .addText(" - ").addValue(PlaylistTrack.TRACK_FK))
            .caption("Playlist tracks");
  }

  private static final class UpdateTotalsProcedure implements DatabaseProcedure<LocalEntityConnection, Void> {

    private static final EntitySelectCondition ALL_INVOICES_CONDITION =
            selectCondition(Invoice.TYPE)
                    .setForUpdate(true).setForeignKeyFetchDepth(0);

    @Override
    public void execute(final LocalEntityConnection entityConnection,
                        final Void... arguments) throws DatabaseException {
      entityConnection.update(entityConnection.getEntities()
              .castTo(Invoice.TYPE, entityConnection.select(ALL_INVOICES_CONDITION)).stream()
              .map(Invoice::updateTotal)
              .filter(Invoice::isModified)
              .collect(Collectors.toList()));
    }
  }

  private static final class RaisePriceFunction implements DatabaseFunction<LocalEntityConnection, Object, List<Entity>> {

    @Override
    public List<Entity> execute(final LocalEntityConnection entityConnection,
                                final Object... arguments) throws DatabaseException {
      List<Long> trackIds = (List<Long>) arguments[0];
      BigDecimal priceIncrease = (BigDecimal) arguments[1];

      EntitySelectCondition selectCondition =
              selectCondition(Track.TYPE, Track.ID, Operator.LIKE, trackIds)
                      .setForUpdate(true);

      List<Track> tracks = entityConnection.getEntities()
              .castTo(Track.TYPE, entityConnection.select(selectCondition));

      tracks.forEach(track -> track.increasePrice(priceIncrease));

      return entityConnection.update(tracks);
    }
  }

  private static final class InvoiceLineTotalProvider
          implements DerivedProperty.Provider<BigDecimal> {

    private static final long serialVersionUID = 1;

    @Override
    public BigDecimal get(final DerivedProperty.SourceValues sourceValues) {
      Integer quantity = sourceValues.get(InvoiceLine.QUANTITY);
      BigDecimal unitPrice = sourceValues.get(InvoiceLine.UNITPRICE);
      if (unitPrice == null || quantity == null) {
        return null;
      }

      return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
  }

  private static final class TrackMinSecProvider
          implements DerivedProperty.Provider<String> {

    private static final long serialVersionUID = 1;

    @Override
    public String get(final DerivedProperty.SourceValues sourceValues) {
      Integer milliseconds = sourceValues.get(Track.MILLISECONDS);
      if (milliseconds == null || milliseconds <= 0) {
        return "";
      }

      return Chinook.getMinutes(milliseconds) + " min " +
              Chinook.getSeconds(milliseconds) + " sec";
    }
  }

  private static final class CoverArtImageProvider
          implements DerivedProperty.Provider<Image> {

    private static final long serialVersionUID = 1;

    @Override
    public Image get(final DerivedProperty.SourceValues sourceValues) {
      byte[] bytes = sourceValues.get(Album.COVER);
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
      StringBuilder builder = new StringBuilder();
      if (customer.isNotNull(Customer.LASTNAME)) {
        builder.append(customer.get(Customer.LASTNAME));
      }
      if (customer.isNotNull(Customer.FIRSTNAME)) {
        builder.append(", ").append(customer.get(Customer.FIRSTNAME));
      }
      if (customer.isNotNull(Customer.EMAIL)) {
        builder.append(" <").append(customer.get(Customer.EMAIL)).append(">");
      }

      return builder.toString();
    }
  }
}