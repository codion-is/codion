/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain.impl;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.formats.LocaleDateTimePattern;
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
import static is.codion.framework.db.condition.Conditions.where;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
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
    define(Artist.TYPE, "chinook.artist",
            primaryKeyProperty(Artist.ID),
            columnProperty(Artist.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160),
            subqueryProperty(Artist.NUMBER_OF_ALBUMS,
                    "select count(*) " +
                            "from chinook.album " +
                            "where album.artistid = artist.artistid"),
            subqueryProperty(Artist.NUMBER_OF_TRACKS,
                    "select count(*) " +
                            "from chinook.track " +
                            "join chinook.album on track.albumid = album.albumid " +
                            "where album.artistid = artist.artistid"))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Artist.NAME))
            .stringFactory(stringFactory(Artist.NAME));
  }

  void album() {
    define(Album.TYPE, "chinook.album",
            primaryKeyProperty(Album.ID),
            columnProperty(Album.ARTIST_ID)
                    .nullable(false),
            foreignKeyProperty(Album.ARTIST_FK)
                    .preferredColumnWidth(160),
            columnProperty(Album.TITLE)
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
                            "where track.albumid = album.albumid"))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Album.ARTIST_ID, Album.TITLE))
            .stringFactory(stringFactory(Album.TITLE));
  }

  void employee() {
    define(Employee.TYPE, "chinook.employee",
            primaryKeyProperty(Employee.ID),
            columnProperty(Employee.LASTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(Employee.FIRSTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(Employee.TITLE)
                    .maximumLength(30),
            columnProperty(Employee.REPORTSTO),
            foreignKeyProperty(Employee.REPORTSTO_FK),
            columnProperty(Employee.BIRTHDATE),
            columnProperty(Employee.HIREDATE)
                    .localeDateTimePattern(LocaleDateTimePattern.builder()
                            .delimiterDot()
                            .yearFourDigits()
                            .build()),
            columnProperty(Employee.ADDRESS)
                    .maximumLength(70),
            columnProperty(Employee.CITY)
                    .maximumLength(40),
            columnProperty(Employee.STATE)
                    .maximumLength(40),
            columnProperty(Employee.COUNTRY)
                    .maximumLength(40),
            columnProperty(Employee.POSTALCODE)
                    .maximumLength(10),
            columnProperty(Employee.PHONE)
                    .maximumLength(24),
            columnProperty(Employee.FAX)
                    .maximumLength(24),
            columnProperty(Employee.EMAIL)
                    .searchProperty()
                    .maximumLength(60))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Employee.LASTNAME, Employee.FIRSTNAME))
            .stringFactory(stringFactory(Employee.LASTNAME)
                    .text(", ").value(Employee.FIRSTNAME));
  }

  void customer() {
    define(Customer.TYPE, "chinook.customer",
            primaryKeyProperty(Customer.ID),
            columnProperty(Customer.LASTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(Customer.FIRSTNAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(40),
            columnProperty(Customer.COMPANY)
                    .maximumLength(80),
            columnProperty(Customer.ADDRESS)
                    .maximumLength(70),
            columnProperty(Customer.CITY)
                    .maximumLength(40),
            columnProperty(Customer.STATE)
                    .maximumLength(40),
            columnProperty(Customer.COUNTRY)
                    .maximumLength(40),
            columnProperty(Customer.POSTALCODE)
                    .maximumLength(10),
            columnProperty(Customer.PHONE)
                    .maximumLength(24),
            columnProperty(Customer.FAX)
                    .maximumLength(24),
            columnProperty(Customer.EMAIL)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(60),
            columnProperty(Customer.SUPPORTREP_ID),
            foreignKeyProperty(Customer.SUPPORTREP_FK))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Customer.LASTNAME, Customer.FIRSTNAME))
            .stringFactory(new CustomerStringProvider());

    defineReport(Customer.REPORT, classPathReport(Chinook.class, "customer_report.jasper"));
  }

  void genre() {
    define(Genre.TYPE, "chinook.genre",
            primaryKeyProperty(Genre.ID),
            columnProperty(Genre.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Genre.NAME))
            .stringFactory(stringFactory(Genre.NAME))
            .smallDataset();
  }

  void mediaType() {
    define(MediaType.TYPE, "chinook.mediatype",
            primaryKeyProperty(MediaType.ID),
            columnProperty(MediaType.NAME)
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(identity())
            .stringFactory(stringFactory(MediaType.NAME))
            .smallDataset();
  }

  void track() {
    define(Track.TYPE, "chinook.track",
            primaryKeyProperty(Track.ID),
            denormalizedViewProperty(Track.ARTIST_DENORM,
                    Track.ALBUM_FK, Album.ARTIST_FK)
                    .preferredColumnWidth(160),
            columnProperty(Track.ALBUM_ID),
            // tag::fetchDepth2[]
            foreignKeyProperty(Track.ALBUM_FK)
                    .fetchDepth(2)
                    .preferredColumnWidth(160),
            // end::fetchDepth2[]
            columnProperty(Track.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(200)
                    .preferredColumnWidth(160),
            columnProperty(Track.GENRE_ID),
            foreignKeyProperty(Track.GENRE_FK),
            columnProperty(Track.COMPOSER)
                    .maximumLength(220)
                    .preferredColumnWidth(160),
            columnProperty(Track.MEDIATYPE_ID)
                    .nullable(false),
            foreignKeyProperty(Track.MEDIATYPE_FK),
            columnProperty(Track.MILLISECONDS)
                    .nullable(false)
                    .format(NumberFormat.getIntegerInstance()),
            derivedProperty(Track.MINUTES_SECONDS_DERIVED,
                    new TrackMinSecProvider(), Track.MILLISECONDS),
            columnProperty(Track.BYTES)
                    .format(NumberFormat.getIntegerInstance()),
            columnProperty(Track.UNITPRICE)
                    .nullable(false)
                    .maximumFractionDigits(2)
                    .beanProperty("unitPrice"))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Track.NAME))
            .stringFactory(stringFactory(Track.NAME));

    defineFunction(Track.RAISE_PRICE, new RaisePriceFunction());
  }

  void invoice() {
    define(Invoice.TYPE, "chinook.invoice",
            primaryKeyProperty(Invoice.ID),
            columnProperty(Invoice.CUSTOMER_ID)
                    .nullable(false),
            foreignKeyProperty(Invoice.CUSTOMER_FK),
            columnProperty(Invoice.DATE)
                    .nullable(false)
                    .defaultValueSupplier(Invoice.DATE_DEFAULT_VALUE)
                    .localeDateTimePattern(LocaleDateTimePattern.builder()
                            .delimiterDot()
                            .yearFourDigits()
                            .build()),
            columnProperty(Invoice.BILLINGADDRESS)
                    .maximumLength(70),
            columnProperty(Invoice.BILLINGCITY)
                    .maximumLength(40),
            columnProperty(Invoice.BILLINGSTATE)
                    .maximumLength(40),
            columnProperty(Invoice.BILLINGCOUNTRY)
                    .maximumLength(40),
            columnProperty(Invoice.BILLINGPOSTALCODE)
                    .maximumLength(10),
            columnProperty(Invoice.TOTAL)
                    .maximumFractionDigits(2)
                    .hidden(),
            subqueryProperty(Invoice.TOTAL_SUBQUERY,
                    "select sum(unitprice * quantity) from chinook.invoiceline " +
                            "where invoiceid = invoice.invoiceid")
                    .maximumFractionDigits(2))
            // tag::identity[]
            .keyGenerator(identity())
            // end::identity[]
            .orderBy(orderBy().ascending(Invoice.CUSTOMER_ID).descending(Invoice.DATE))
            .stringFactory(stringFactory(Invoice.ID));

    defineFunction(Invoice.UPDATE_TOTALS, new UpdateTotalsFunction());
  }

  void invoiceLine() {
    define(InvoiceLine.TYPE, "chinook.invoiceline",
            primaryKeyProperty(InvoiceLine.ID),
            columnProperty(InvoiceLine.INVOICE_ID),
            // tag::fetchDepth0[]
            foreignKeyProperty(InvoiceLine.INVOICE_FK)
                    .fetchDepth(0)
                    .nullable(false),
            columnProperty(InvoiceLine.TRACK_ID),
            // end::fetchDepth0[]
            foreignKeyProperty(InvoiceLine.TRACK_FK)
                    .nullable(false)
                    .preferredColumnWidth(100),
            denormalizedProperty(InvoiceLine.UNITPRICE,
                    InvoiceLine.TRACK_FK, Track.UNITPRICE)
                    .nullable(false),
            columnProperty(InvoiceLine.QUANTITY)
                    .nullable(false)
                    .defaultValue(1),
            derivedProperty(InvoiceLine.TOTAL, new InvoiceLineTotalProvider(),
                    InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE))
            .keyGenerator(identity());
  }

  void playlist() {
    define(Playlist.TYPE, "chinook.playlist",
            primaryKeyProperty(Playlist.ID),
            columnProperty(Playlist.NAME)
                    .searchProperty()
                    .nullable(false)
                    .maximumLength(120)
                    .preferredColumnWidth(160))
            .keyGenerator(identity())
            .orderBy(orderBy().ascending(Playlist.NAME))
            .stringFactory(stringFactory(Playlist.NAME));
  }

  void playlistTrack() {
    define(PlaylistTrack.TYPE, "chinook.playlisttrack",
            primaryKeyProperty(PlaylistTrack.ID),
            columnProperty(PlaylistTrack.PLAYLIST_ID)
                    .nullable(false),
            foreignKeyProperty(PlaylistTrack.PLAYLIST_FK)
                    .preferredColumnWidth(120),
            denormalizedViewProperty(PlaylistTrack.ARTIST_DENORM,
                    PlaylistTrack.ALBUM_DENORM, Album.ARTIST_FK)
                    .preferredColumnWidth(160),
            columnProperty(PlaylistTrack.TRACK_ID)
                    .nullable(false),
            foreignKeyProperty(PlaylistTrack.TRACK_FK)
                    .fetchDepth(3)
                    .preferredColumnWidth(160),
            denormalizedViewProperty(PlaylistTrack.ALBUM_DENORM,
                    PlaylistTrack.TRACK_FK, Track.ALBUM_FK)
                    .preferredColumnWidth(160))
            .keyGenerator(identity())
            .stringFactory(stringFactory(PlaylistTrack.PLAYLIST_FK)
                    .text(" - ").value(PlaylistTrack.TRACK_FK));
  }

  private static final class UpdateTotalsFunction implements DatabaseFunction<EntityConnection, Object, List<Entity>> {

    private static final SelectCondition ALL_INVOICES = condition(Invoice.TYPE)
            .asSelectCondition().forUpdate().fetchDepth(0);

    @Override
    public List<Entity> execute(final EntityConnection entityConnection,
                                final List<Object> arguments) throws DatabaseException {
      return entityConnection.update(Entity.castTo(Invoice.TYPE,
              entityConnection.select(ALL_INVOICES)).stream()
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

      SelectCondition selectCondition = where(Track.ID).equalTo(trackIds)
              .asSelectCondition().forUpdate();

      return entityConnection.update(Entity.castTo(Track.TYPE,
              entityConnection.select(selectCondition)).stream()
              .map(track -> track.raisePrice(priceIncrease))
              .collect(Collectors.toList()));
    }
  }
}