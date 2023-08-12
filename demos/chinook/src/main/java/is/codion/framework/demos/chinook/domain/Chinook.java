/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain;

import is.codion.common.db.operation.FunctionType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.DerivedProperty.SourceValues;
import is.codion.framework.domain.property.Property;
import is.codion.plugin.jasperreports.model.JRReportType;
import is.codion.plugin.jasperreports.model.JasperReports;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.regex.Pattern;

import static is.codion.common.db.operation.FunctionType.functionType;
import static is.codion.framework.domain.DomainType.domainType;
import static java.util.Objects.requireNonNull;

public interface Chinook {

  DomainType DOMAIN = domainType(Chinook.class);

  interface Artist {
    EntityType TYPE = DOMAIN.entityType("artist@chinook", Artist.class.getName());

    Column<Long> ID = TYPE.longColumn("artistid");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<Integer> NUMBER_OF_ALBUMS = TYPE.integerColumn("number_of_albums");
    Column<Integer> NUMBER_OF_TRACKS = TYPE.integerColumn("number_of_tracks");
  }

  interface Album {
    EntityType TYPE = DOMAIN.entityType("album@chinook", Album.class.getName());

    Column<Long> ID = TYPE.longColumn("albumid");
    Column<String> TITLE = TYPE.stringColumn("title");
    Column<Long> ARTIST_ID = TYPE.longColumn("artistid");
    Column<byte[]> COVER = TYPE.byteArrayColumn("cover");
    Attribute<Image> COVERIMAGE = TYPE.attribute("coverimage", Image.class);
    Column<Integer> NUMBER_OF_TRACKS = TYPE.integerColumn("number_of_tracks");

    ForeignKey ARTIST_FK = TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
  }

  interface Employee {
    EntityType TYPE = DOMAIN.entityType("employee@chinook", Employee.class.getName());

    Column<Long> ID = TYPE.longColumn("employeeid");
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

    ForeignKey REPORTSTO_FK = TYPE.foreignKey("reportsto_fk", REPORTSTO, Employee.ID);
  }

  interface Customer {
    EntityType TYPE = DOMAIN.entityType("customer@chinook", Customer.class.getName());

    Column<Long> ID = TYPE.longColumn("customerid");
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
    Column<Long> SUPPORTREP_ID = TYPE.longColumn("supportrepid");

    ForeignKey SUPPORTREP_FK = TYPE.foreignKey("supportrep_fk", SUPPORTREP_ID, Employee.ID);

    JRReportType REPORT = JasperReports.reportType("customer_report");
  }

  interface Genre {
    EntityType TYPE = DOMAIN.entityType("genre@chinook", Genre.class.getName());

    Column<Long> ID = TYPE.longColumn("genreid");
    Column<String> NAME = TYPE.stringColumn("name");
  }

  interface MediaType {
    EntityType TYPE = DOMAIN.entityType("mediatype@chinook", MediaType.class.getName());

    Column<Long> ID = TYPE.longColumn("mediatypeid");
    Column<String> NAME = TYPE.stringColumn("name");
  }

  interface Track extends Entity {
    EntityType TYPE = DOMAIN.entityType("track@chinook", Track.class, Track.class.getName());

    Column<Long> ID = TYPE.longColumn("trackid");
    Column<String> NAME = TYPE.stringColumn("name");
    Attribute<Entity> ARTIST_DENORM = TYPE.entityAttribute("artist_denorm");
    Column<Long> ALBUM_ID = TYPE.longColumn("albumid");
    Column<Long> MEDIATYPE_ID = TYPE.longColumn("mediatypeid");
    Column<Long> GENRE_ID = TYPE.longColumn("genreid");
    Column<String> COMPOSER = TYPE.stringColumn("composer");
    Column<Integer> MILLISECONDS = TYPE.integerColumn("milliseconds");
    Attribute<String> MINUTES_SECONDS_DERIVED = TYPE.stringAttribute("minutes_seconds_derived");
    Column<Integer> BYTES = TYPE.integerColumn("bytes");
    Column<BigDecimal> UNITPRICE = TYPE.bigDecimalColumn("unitprice");
    Column<Void> RANDOM = TYPE.column("random()", Void.class);

    ForeignKey ALBUM_FK = TYPE.foreignKey("album_fk", ALBUM_ID, Album.ID);
    ForeignKey MEDIATYPE_FK = TYPE.foreignKey("mediatype_fk", MEDIATYPE_ID, MediaType.ID);
    ForeignKey GENRE_FK = TYPE.foreignKey("genre_fk", GENRE_ID, Genre.ID);

    FunctionType<EntityConnection, RaisePriceParameters, Collection<Entity>> RAISE_PRICE = functionType("chinook.raise_price");

    default void raisePrice(BigDecimal priceIncrease) {
      put(UNITPRICE, get(UNITPRICE).add(priceIncrease));
    }

    final class RaisePriceParameters implements Serializable {

      private static final long serialVersionUID = 1;

      private final Collection<Long> trackIds;
      private final BigDecimal priceIncrease;

      public RaisePriceParameters(Collection<Long> trackIds, BigDecimal priceIncrease) {
        this.trackIds = requireNonNull(trackIds);
        this.priceIncrease = requireNonNull(priceIncrease);
      }

      public Collection<Long> trackIds() {
        return trackIds;
      }

      public BigDecimal priceIncrease() {
        return priceIncrease;
      }
    }
  }

  interface Invoice extends Entity {
    EntityType TYPE = DOMAIN.entityType("invoice@chinook", Invoice.class, Invoice.class.getName());

    Column<Long> ID = TYPE.longColumn("invoiceid");
    Column<Long> CUSTOMER_ID = TYPE.longColumn("customerid");
    Column<LocalDate> DATE = TYPE.localDateColumn("invoicedate");
    Column<String> BILLINGADDRESS = TYPE.stringColumn("billingaddress");
    Column<String> BILLINGCITY = TYPE.stringColumn("billingcity");
    Column<String> BILLINGSTATE = TYPE.stringColumn("billingstate");
    Column<String> BILLINGCOUNTRY = TYPE.stringColumn("billingcountry");
    Column<String> BILLINGPOSTALCODE = TYPE.stringColumn("billingpostalcode");
    Column<BigDecimal> TOTAL = TYPE.bigDecimalColumn("total");
    Column<BigDecimal> CALCULATED_TOTAL = TYPE.bigDecimalColumn("calculated_total");

    ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);

    FunctionType<EntityConnection, Collection<Long>, Collection<Entity>> UPDATE_TOTALS = functionType("chinook.update_totals");

    Property.ValueSupplier<LocalDate> DATE_DEFAULT_VALUE = LocalDate::now;

    default void updateTotal() {
      put(TOTAL, optional(CALCULATED_TOTAL).orElse(BigDecimal.ZERO));
    }
  }

  interface InvoiceLine {
    EntityType TYPE = DOMAIN.entityType("invoiceline@chinook", InvoiceLine.class.getName());

    Column<Long> ID = TYPE.longColumn("invoicelineid");
    Column<Long> INVOICE_ID = TYPE.longColumn("invoiceid");
    Column<Long> TRACK_ID = TYPE.longColumn("trackid");
    Column<BigDecimal> UNITPRICE = TYPE.bigDecimalColumn("unitprice");
    Column<Integer> QUANTITY = TYPE.integerColumn("quantity");
    Attribute<BigDecimal> TOTAL = TYPE.bigDecimalAttribute("total");

    ForeignKey INVOICE_FK = TYPE.foreignKey("invoice_fk", INVOICE_ID, Invoice.ID);
    ForeignKey TRACK_FK = TYPE.foreignKey("track_fk", TRACK_ID, Track.ID);
  }

  interface Playlist {
    EntityType TYPE = DOMAIN.entityType("playlist@chinook", Playlist.class.getName());

    Column<Long> ID = TYPE.longColumn("playlistid");
    Column<String> NAME = TYPE.stringColumn("name");

    FunctionType<EntityConnection, RandomPlaylistParameters, Entity> RANDOM_PLAYLIST = functionType("chinook.random_playlist");

    final class RandomPlaylistParameters implements Serializable {

      private static final long serialVersionUID = 1;

      private final String playlistName;
      private final Integer noOfTracks;
      private final Collection<Entity> genres;

      public RandomPlaylistParameters(String playlistName, Integer noOfTracks, Collection<Entity> genres) {
        this.playlistName = playlistName;
        this.noOfTracks = noOfTracks;
        this.genres = requireNonNull(genres);
      }

      public String playlistName() {
        return playlistName;
      }

      public Integer noOfTracks() {
        return noOfTracks;
      }

      public Collection<Entity> genres() {
        return genres;
      }
    }
  }

  interface PlaylistTrack {
    EntityType TYPE = DOMAIN.entityType("playlisttrack@chinook", PlaylistTrack.class.getName());

    Column<Long> ID = TYPE.longColumn("playlisttrackid");
    Column<Long> PLAYLIST_ID = TYPE.longColumn("playlistid");
    Column<Long> TRACK_ID = TYPE.longColumn("trackid");
    Attribute<Entity> ALBUM_DENORM = TYPE.entityAttribute("album_denorm");
    Attribute<Entity> ARTIST_DENORM = TYPE.entityAttribute("artist_denorm");

    ForeignKey PLAYLIST_FK = TYPE.foreignKey("playlist_fk", PLAYLIST_ID, Playlist.ID);
    ForeignKey TRACK_FK = TYPE.foreignKey("track_fk", TRACK_ID, Track.ID);
  }

  static Integer minutes(Integer milliseconds) {
    if (milliseconds == null) {
      return null;
    }

    return milliseconds / 1000 / 60;
  }

  static Integer seconds(Integer milliseconds) {
    if (milliseconds == null) {
      return null;
    }

    return milliseconds / 1000 % 60;
  }

  static Integer milliseconds(Integer minutes, Integer seconds) {
    int milliseconds = minutes == null ? 0 : minutes * 60 * 1000;
    milliseconds += seconds == null ? 0 : seconds * 1000;

    return milliseconds == 0 ? null : milliseconds;
  }

  final class InvoiceLineTotalProvider
          implements DerivedProperty.Provider<BigDecimal> {

    private static final long serialVersionUID = 1;

    @Override
    public BigDecimal get(SourceValues sourceValues) {
      Integer quantity = sourceValues.get(InvoiceLine.QUANTITY);
      BigDecimal unitPrice = sourceValues.get(InvoiceLine.UNITPRICE);
      if (unitPrice == null || quantity == null) {
        return null;
      }

      return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
  }

  final class TrackMinSecProvider
          implements DerivedProperty.Provider<String> {

    private static final long serialVersionUID = 1;

    @Override
    public String get(SourceValues sourceValues) {
      return sourceValues.optional(Track.MILLISECONDS)
              .map(TrackMinSecProvider::toMinutesSecondsString)
              .orElse(null);
    }

    private static String toMinutesSecondsString(Integer milliseconds) {
      return minutes(milliseconds) + " min " +
              seconds(milliseconds) + " sec";
    }
  }

  final class CoverArtImageProvider
          implements DerivedProperty.Provider<Image> {

    private static final long serialVersionUID = 1;

    @Override
    public Image get(SourceValues sourceValues) {
      return sourceValues.optional(Album.COVER)
              .map(CoverArtImageProvider::fromBytes)
              .orElse(null);
    }

    private static Image fromBytes(byte[] bytes) {
      try {
        return ImageIO.read(new ByteArrayInputStream(bytes));
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  final class CustomerStringProvider
          implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    @Override
    public String apply(Entity customer) {
      return new StringBuilder()
              .append(customer.get(Customer.LASTNAME))
              .append(", ")
              .append(customer.get(Customer.FIRSTNAME))
              .append(customer.optional(Customer.EMAIL)
                      .map(email -> " <" + email + ">")
                      .orElse(""))
              .toString();
    }
  }

  final class CoverFormatter extends Format {

    private final NumberFormat kbFormat = NumberFormat.getIntegerInstance();

    @Override
    public StringBuffer format(Object value, StringBuffer toAppendTo, FieldPosition pos) {
      if (value != null) {
        toAppendTo.append(kbFormat.format(((byte[]) value).length / 1024) + " Kb");
      }

      return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
      throw new UnsupportedOperationException();
    }
  }

  final class EmailValidator extends DefaultEntityValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(.+)$");
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(Chinook.class.getName());

    private final Column<String> emailColumn;

    public EmailValidator(Column<String> emailColumn) {
      this.emailColumn = emailColumn;
    }

    @Override
    public <T> void validate(Entity entity, Attribute<T> attribute) throws ValidationException {
      super.validate(entity, attribute);
      if (attribute.equals(emailColumn)) {
        validateEmail(entity.get(emailColumn));
      }
    }

    private void validateEmail(String email) throws ValidationException {
      if (!EMAIL_PATTERN.matcher(email).matches()) {
        throw new ValidationException(emailColumn, email, BUNDLE.getString("invalid_email"));
      }
    }
  }
}
