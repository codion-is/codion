/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain.impl;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.entity.Entity;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.stream.Collectors;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
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
  }

  void artist() {
    final String bundleName = Artist.class.getName();

    define(Artist.TYPE, "chinook.artist",
            primaryKeyProperty(Artist.ID),
            columnProperty(Artist.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName),
            subqueryProperty(Artist.NUMBER_OF_ALBUMS,
                    "select count(*) " +
                            "from chinook.album " +
                            "where album.artistid = artist.artistid")
                    .captionResource(bundleName),
            subqueryProperty(Artist.NUMBER_OF_TRACKS,
                    "select count(*) " +
                            "from chinook.track " +
                            "where track.albumid in (" +
                            "  select albumid " +
                            "  from chinook.album " +
                            "  where album.artistid = artist.artistid" +
                            ")")
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.artist"))
            .orderBy(orderBy().ascending(Artist.NAME))
            .stringFactory(stringFactory(Artist.NAME))
            .captionResource(bundleName);
  }

  void album() {
    final String bundleName = Album.class.getName();

    define(Album.TYPE, "chinook.album",
            primaryKeyProperty(Album.ID),
            columnProperty(Album.ARTIST_ID)
                    .nullable(false),
            foreignKeyProperty(Album.ARTIST_FK)
                    .captionResource(bundleName)
                    .preferredColumnWidth(160),
            columnProperty(Album.TITLE)
                    .captionResource(bundleName)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(160)
                    .preferredColumnWidth(160),
            blobProperty(Album.COVER)
                    .eagerlyLoaded(),
            derivedProperty(Album.COVERIMAGE,
                    new CoverArtImageProvider(), Album.COVER),
            subqueryProperty(Album.NUMBER_OF_TRACKS,
                    "select count(*) " +
                            "from chinook.track " +
                            "where track.albumid = album.albumid")
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.album"))
            .orderBy(orderBy().ascending(Album.ARTIST_ID, Album.TITLE))
            .stringFactory(stringFactory(Album.TITLE))
            .captionResource(bundleName);
  }

  void employee() {
    final String bundleName = Employee.class.getName();

    define(Employee.TYPE, "chinook.employee",
            primaryKeyProperty(Employee.ID),
            columnProperty(Employee.LASTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(20)
                    .captionResource(bundleName),
            columnProperty(Employee.FIRSTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(20)
                    .captionResource(bundleName),
            columnProperty(Employee.TITLE)
                    .maximumLength(30)
                    .captionResource(bundleName),
            columnProperty(Employee.REPORTSTO),
            foreignKeyProperty(Employee.REPORTSTO_FK)
                    .captionResource(bundleName),
            columnProperty(Employee.BIRTHDATE)
                    .captionResource(bundleName),
            columnProperty(Employee.HIREDATE)
                    .captionResource(bundleName),
            columnProperty(Employee.ADDRESS)
                    .maximumLength(70)
                    .captionResource(bundleName),
            columnProperty(Employee.CITY)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Employee.STATE)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Employee.COUNTRY)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Employee.POSTALCODE)
                    .maximumLength(10)
                    .captionResource(bundleName),
            columnProperty(Employee.PHONE)
                    .maximumLength(24)
                    .captionResource(bundleName),
            columnProperty(Employee.FAX)
                    .maximumLength(24)
                    .captionResource(bundleName),
            columnProperty(Employee.EMAIL)
                    .searchProperty()
                    .maximumLength(60)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.employee"))
            .orderBy(orderBy().ascending(Employee.LASTNAME, Employee.FIRSTNAME))
            .stringFactory(stringFactory(Employee.LASTNAME)
                    .text(", ").value(Employee.FIRSTNAME))
            .captionResource(bundleName);
  }

  void customer() {
    final String bundleName = Customer.class.getName();

    define(Customer.TYPE, "chinook.customer",
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.LASTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(20)
                    .captionResource(bundleName),
            columnProperty(Customer.FIRSTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Customer.COMPANY)
                    .maximumLength(80)
                    .captionResource(bundleName),
            columnProperty(Customer.ADDRESS)
                    .maximumLength(70)
                    .captionResource(bundleName),
            columnProperty(Customer.CITY)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Customer.STATE)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Customer.COUNTRY)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Customer.POSTALCODE)
                    .maximumLength(10)
                    .captionResource(bundleName),
            columnProperty(Customer.PHONE)
                    .maximumLength(24)
                    .captionResource(bundleName),
            columnProperty(Customer.FAX)
                    .maximumLength(24)
                    .captionResource(bundleName),
            columnProperty(Customer.EMAIL)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(60)
                    .captionResource(bundleName),
            columnProperty(Customer.SUPPORTREP_ID),
            foreignKeyProperty(Customer.SUPPORTREP_FK)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.customer"))
            .orderBy(orderBy().ascending(Customer.LASTNAME, Customer.FIRSTNAME))
            .stringFactory(new CustomerStringProvider())
            .captionResource(bundleName);

    defineReport(Customer.REPORT, classPathReport(Chinook.class, "customer_report.jasper"));
  }

  void genre() {
    final String bundleName = Genre.class.getName();

    define(Genre.TYPE, "chinook.genre",
            primaryKeyProperty(Genre.ID),
            columnProperty(Genre.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.genre"))
            .orderBy(orderBy().ascending(Genre.NAME))
            .stringFactory(stringFactory(Genre.NAME))
            .smallDataset()
            .captionResource(bundleName);
  }

  void mediaType() {
    final String bundleName = MediaType.class.getName();

    define(MediaType.TYPE, "chinook.mediatype",
            primaryKeyProperty(MediaType.ID),
            columnProperty(MediaType.NAME)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.mediatype"))
            .stringFactory(stringFactory(MediaType.NAME))
            .smallDataset()
            .captionResource(bundleName);
  }

  void track() {
    final String bundleName = Track.class.getName();

    define(Track.TYPE, "chinook.track",
            primaryKeyProperty(Track.ID),
            denormalizedViewProperty(Track.ARTIST_DENORM,
                    Track.ALBUM_FK, Album.ARTIST_FK)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName),
            columnProperty(Track.ALBUM_ID),
            // tag::fetchDepth2[]
            foreignKeyProperty(Track.ALBUM_FK)
                    .fetchDepth(2)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName),
            // end::fetchDepth2[]
            columnProperty(Track.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(200)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName),
            columnProperty(Track.GENRE_ID),
            foreignKeyProperty(Track.GENRE_FK)
                    .captionResource(bundleName),
            columnProperty(Track.COMPOSER)
                    .maximumLength(220)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName),
            columnProperty(Track.MEDIATYPE_ID)
                    .nullable(false),
            foreignKeyProperty(Track.MEDIATYPE_FK)
                    .captionResource(bundleName),
            columnProperty(Track.MILLISECONDS)
                    .nullable(false)
                    .format(NumberFormat.getIntegerInstance())
                    .captionResource(bundleName),
            derivedProperty(Track.MINUTES_SECONDS_DERIVED,
                    new TrackMinSecProvider(), Track.MILLISECONDS),
            columnProperty(Track.BYTES)
                    .format(NumberFormat.getIntegerInstance())
                    .captionResource(bundleName),
            columnProperty(Track.UNITPRICE)
                    .nullable(false)
                    .maximumFractionDigits(2)
                    .beanProperty("unitPrice")
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.track"))
            .orderBy(orderBy().ascending(Track.NAME))
            .stringFactory(stringFactory(Track.NAME))
            .captionResource(bundleName);

    defineFunction(Track.RAISE_PRICE, new RaisePriceFunction());
  }

  void invoice() {
    final String bundleName = Invoice.class.getName();

    define(Invoice.TYPE, "chinook.invoice",
            primaryKeyProperty(Invoice.ID)
                    .captionResource(bundleName),
            columnProperty(Invoice.CUSTOMER_ID)
                    .nullable(false),
            foreignKeyProperty(Invoice.CUSTOMER_FK)
                    .captionResource(bundleName),
            columnProperty(Invoice.INVOICEDATE)
                    .nullable(false)
                    .captionResource(bundleName),
            columnProperty(Invoice.BILLINGADDRESS)
                    .maximumLength(70)
                    .captionResource(bundleName),
            columnProperty(Invoice.BILLINGCITY)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Invoice.BILLINGSTATE)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Invoice.BILLINGCOUNTRY)
                    .maximumLength(40)
                    .captionResource(bundleName),
            columnProperty(Invoice.BILLINGPOSTALCODE)
                    .maximumLength(10)
                    .captionResource(bundleName),
            columnProperty(Invoice.TOTAL)
                    .maximumFractionDigits(2)
                    .captionResource(bundleName)
                    .hidden(),
            subqueryProperty(Invoice.TOTAL_SUBQUERY,
                    "select sum(unitprice * quantity) from chinook.invoiceline " +
                            "where invoiceid = invoice.invoiceid")
                    .maximumFractionDigits(2)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.invoice"))
            .orderBy(orderBy().ascending(Invoice.CUSTOMER_ID).descending(Invoice.INVOICEDATE))
            .stringFactory(stringFactory(Invoice.ID))
            .captionResource(bundleName);

    defineProcedure(Invoice.UPDATE_TOTALS, new UpdateTotalsProcedure());
  }

  void invoiceLine() {
    final String bundleName = InvoiceLine.class.getName();

    define(InvoiceLine.TYPE, "chinook.invoiceline",
            primaryKeyProperty(InvoiceLine.ID),
            columnProperty(InvoiceLine.INVOICE_ID),
            // tag::fetchDepth0[]
            foreignKeyProperty(InvoiceLine.INVOICE_FK)
                    .fetchDepth(0)
                    .nullable(false)
                    .captionResource(bundleName),
            columnProperty(InvoiceLine.TRACK_ID),
            // end::fetchDepth0[]
            foreignKeyProperty(InvoiceLine.TRACK_FK)
                    .nullable(false)
                    .preferredColumnWidth(100)
                    .captionResource(bundleName),
            denormalizedProperty(InvoiceLine.UNITPRICE,
                    InvoiceLine.TRACK_FK, Track.UNITPRICE)
                    .nullable(false)
                    .captionResource(bundleName),
            columnProperty(InvoiceLine.QUANTITY)
                    .nullable(false)
                    .defaultValue(1)
                    .captionResource(bundleName),
            derivedProperty(InvoiceLine.TOTAL, new InvoiceLineTotalProvider(),
                    InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.invoiceline"))
            .captionResource(bundleName);
  }

  void playlist() {
    final String bundleName = Playlist.class.getName();

    define(Playlist.TYPE, "chinook.playlist",
            primaryKeyProperty(Playlist.ID),
            columnProperty(Playlist.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.playlist"))
            .orderBy(orderBy().ascending(Playlist.NAME))
            .stringFactory(stringFactory(Playlist.NAME))
            .captionResource(bundleName);
  }

  void playlistTrack() {
    final String bundleName = PlaylistTrack.class.getName();

    define(PlaylistTrack.TYPE, "chinook.playlisttrack",
            primaryKeyProperty(PlaylistTrack.ID),
            columnProperty(PlaylistTrack.PLAYLIST_ID)
                    .nullable(false),
            foreignKeyProperty(PlaylistTrack.PLAYLIST_FK)
                    .preferredColumnWidth(120)
                    .captionResource(bundleName),
            denormalizedViewProperty(PlaylistTrack.ARTIST_DENORM,
                    PlaylistTrack.ALBUM_DENORM, Album.ARTIST_FK)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName),
            columnProperty(PlaylistTrack.TRACK_ID)
                    .nullable(false),
            foreignKeyProperty(PlaylistTrack.TRACK_FK)
                    .fetchDepth(3)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName),
            denormalizedViewProperty(PlaylistTrack.ALBUM_DENORM,
                    PlaylistTrack.TRACK_FK, Track.ALBUM_FK)
                    .preferredColumnWidth(160)
                    .captionResource(bundleName))
            .keyGenerator(automatic("chinook.playlisttrack"))
            .stringFactory(stringFactory(PlaylistTrack.PLAYLIST_FK)
                    .text(" - ").value(PlaylistTrack.TRACK_FK))
            .captionResource(bundleName);
  }

  private static final class UpdateTotalsProcedure implements DatabaseProcedure<EntityConnection, Object> {

    private static final SelectCondition ALL_INVOICES =
            condition(Invoice.TYPE).select()
                    .forUpdate().fetchDepth(0);

    @Override
    public void execute(final EntityConnection entityConnection,
                        final List<Object> arguments) throws DatabaseException {
      entityConnection.update(entityConnection.getEntities()
              .castTo(Invoice.TYPE, entityConnection.select(ALL_INVOICES)).stream()
              .map(Invoice::updateTotal)
              .filter(Invoice::isModified)
              .collect(Collectors.toList()));
    }
  }

  private static final class RaisePriceFunction implements DatabaseFunction<EntityConnection, Object, List<Entity>> {

    @Override
    public List<Entity> execute(final EntityConnection entityConnection,
                                final List<Object> arguments) throws DatabaseException {
      List<Long> trackIds = (List<Long>) arguments.get(0);
      BigDecimal priceIncrease = (BigDecimal) arguments.get(1);

      SelectCondition selectCondition =
              condition(Track.ID).equalTo(trackIds).select()
                      .forUpdate();

      return entityConnection.update(entityConnection.getEntities()
              .castTo(Track.TYPE, entityConnection.select(selectCondition)).stream()
              .map(track -> track.raisePrice(priceIncrease))
              .collect(Collectors.toList()));
    }
  }
}