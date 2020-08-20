/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.plugin.jasperreports.model.JRReportType;
import is.codion.plugin.jasperreports.model.JasperReports;

import java.awt.Image;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static is.codion.common.db.operation.FunctionType.functionType;
import static is.codion.common.db.operation.ProcedureType.procedureType;
import static is.codion.framework.domain.DomainType.domainType;

public interface Chinook {

  DomainType DOMAIN = domainType("ChinookImpl");

  interface Artist {
    EntityType<Entity> TYPE = DOMAIN.entityType("artist@chinook");
    Attribute<Long> ID = TYPE.longAttribute("artistid");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<Integer> NUMBER_OF_ALBUMS = TYPE.integerAttribute("number_of_albums");
    Attribute<Integer> NUMBER_OF_TRACKS = TYPE.integerAttribute("number_of_tracks");
  }

  interface Album {
    EntityType<Entity> TYPE = DOMAIN.entityType("album@chinook");
    Attribute<Long> ID = TYPE.longAttribute("albumid");
    Attribute<String> TITLE = TYPE.stringAttribute("title");
    Attribute<Long> ARTIST_ID = TYPE.longAttribute("artistid");
    Attribute<Entity> ARTIST_FK = TYPE.entityAttribute("artist_fk");
    Attribute<byte[]> COVER = TYPE.byteArrayAttribute("cover");
    Attribute<Image> COVERIMAGE = TYPE.attribute("coverimage", Image.class);
    Attribute<Integer> NUMBER_OF_TRACKS = TYPE.integerAttribute("number_of_tracks");
  }

  interface Employee {
    EntityType<Entity> TYPE = DOMAIN.entityType("employee@chinook");
    Attribute<Long> ID = TYPE.longAttribute("employeeid");
    Attribute<String> LASTNAME = TYPE.stringAttribute("lastname");
    Attribute<String> FIRSTNAME = TYPE.stringAttribute("firstname");
    Attribute<String> TITLE = TYPE.stringAttribute("title");
    Attribute<Long> REPORTSTO = TYPE.longAttribute("reportsto");
    Attribute<Entity> REPORTSTO_FK = TYPE.entityAttribute("reportsto_fk");
    Attribute<LocalDate> BIRTHDATE = TYPE.localDateAttribute("birthdate");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");
    Attribute<String> ADDRESS = TYPE.stringAttribute("address");
    Attribute<String> CITY = TYPE.stringAttribute("city");
    Attribute<String> STATE = TYPE.stringAttribute("state");
    Attribute<String> COUNTRY = TYPE.stringAttribute("country");
    Attribute<String> POSTALCODE = TYPE.stringAttribute("postalcode");
    Attribute<String> PHONE = TYPE.stringAttribute("phone");
    Attribute<String> FAX = TYPE.stringAttribute("fax");
    Attribute<String> EMAIL = TYPE.stringAttribute("email");
  }

  interface Customer {
    EntityType<Entity> TYPE = DOMAIN.entityType("customer@chinook");
    Attribute<Long> ID = TYPE.longAttribute("customerid");
    Attribute<String> FIRSTNAME = TYPE.stringAttribute("firstname");
    Attribute<String> LASTNAME = TYPE.stringAttribute("lastname");
    Attribute<String> COMPANY = TYPE.stringAttribute("company");
    Attribute<String> ADDRESS = TYPE.stringAttribute("address");
    Attribute<String> CITY = TYPE.stringAttribute("city");
    Attribute<String> STATE = TYPE.stringAttribute("state");
    Attribute<String> COUNTRY = TYPE.stringAttribute("country");
    Attribute<String> POSTALCODE = TYPE.stringAttribute("postalcode");
    Attribute<String> PHONE = TYPE.stringAttribute("phone");
    Attribute<String> FAX = TYPE.stringAttribute("fax");
    Attribute<String> EMAIL = TYPE.stringAttribute("email");
    Attribute<Long> SUPPORTREP_ID = TYPE.longAttribute("supportrepid");
    Attribute<Entity> SUPPORTREP_FK = TYPE.entityAttribute("supportrep_fk");

    JRReportType REPORT = JasperReports.reportType("customer_report");
  }

  interface Genre {
    EntityType<Entity> TYPE = DOMAIN.entityType("genre@chinook");
    Attribute<Long> ID = TYPE.longAttribute("genreid");
    Attribute<String> NAME = TYPE.stringAttribute("name");
  }

  interface MediaType {
    EntityType<Entity> TYPE = DOMAIN.entityType("mediatype@chinook");
    Attribute<Long> ID = TYPE.longAttribute("mediatypeid");
    Attribute<String> NAME = TYPE.stringAttribute("name");
  }

  interface Track extends Entity {
    EntityType<Track> TYPE = DOMAIN.entityType("track@chinook", Track.class);
    Attribute<Long> ID = TYPE.longAttribute("trackid");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<Entity> ARTIST_DENORM = TYPE.entityAttribute("artist_denorm");
    Attribute<Long> ALBUM_ID = TYPE.longAttribute("albumid");
    Attribute<Entity> ALBUM_FK = TYPE.entityAttribute("album_fk");
    Attribute<Long> MEDIATYPE_ID = TYPE.longAttribute("mediatypeid");
    Attribute<Entity> MEDIATYPE_FK = TYPE.entityAttribute("mediatype_fk");
    Attribute<Long> GENRE_ID = TYPE.longAttribute("genreid");
    Attribute<Entity> GENRE_FK = TYPE.entityAttribute("genre_fk");
    Attribute<String> COMPOSER = TYPE.stringAttribute("composer");
    Attribute<Integer> MILLISECONDS = TYPE.integerAttribute("milliseconds");
    Attribute<String> MINUTES_SECONDS_DERIVED = TYPE.stringAttribute("minutes_seconds_derived");
    Attribute<Integer> BYTES = TYPE.integerAttribute("bytes");
    Attribute<BigDecimal> UNITPRICE = TYPE.bigDecimalAttribute("unitprice");

    FunctionType<EntityConnection, Object, List<Entity>> RAISE_PRICE = functionType("chinook.raise_price_function");

    default Track raisePrice(final BigDecimal priceIncrease) {
      put(UNITPRICE, get(UNITPRICE).add(priceIncrease));
      return this;
    }
  }

  interface Invoice extends Entity {
    EntityType<Invoice> TYPE = DOMAIN.entityType("invoice@chinook", Invoice.class);
    Attribute<Long> ID = TYPE.longAttribute("invoiceid");
    Attribute<Long> CUSTOMER_ID = TYPE.longAttribute("customerid");
    Attribute<Entity> CUSTOMER_FK = TYPE.entityAttribute("customer_fk");
    Attribute<LocalDateTime> INVOICEDATE = TYPE.localDateTimeAttribute("invoicedate");
    Attribute<String> BILLINGADDRESS = TYPE.stringAttribute("billingaddress");
    Attribute<String> BILLINGCITY = TYPE.stringAttribute("billingcity");
    Attribute<String> BILLINGSTATE = TYPE.stringAttribute("billingstate");
    Attribute<String> BILLINGCOUNTRY = TYPE.stringAttribute("billingcountry");
    Attribute<String> BILLINGPOSTALCODE = TYPE.stringAttribute("billingpostalcode");
    Attribute<BigDecimal> TOTAL = TYPE.bigDecimalAttribute("total");
    Attribute<BigDecimal> TOTAL_SUBQUERY = TYPE.bigDecimalAttribute("total_subquery");

    ProcedureType<EntityConnection, Object> UPDATE_TOTALS = procedureType("chinook.update_totals_procedure");

    default Invoice updateTotal() {
      put(TOTAL, get(TOTAL_SUBQUERY));
      return this;
    }
  }

  interface InvoiceLine {
    EntityType<Entity> TYPE = DOMAIN.entityType("invoiceline@chinook");
    Attribute<Long> ID = TYPE.longAttribute("invoicelineid");
    Attribute<Long> INVOICE_ID = TYPE.longAttribute("invoiceid");
    Attribute<Entity> INVOICE_FK = TYPE.entityAttribute("invoice_fk");
    Attribute<Long> TRACK_ID = TYPE.longAttribute("trackid");
    Attribute<Entity> TRACK_FK = TYPE.entityAttribute("track_fk");
    Attribute<BigDecimal> UNITPRICE = TYPE.bigDecimalAttribute("unitprice");
    Attribute<Integer> QUANTITY = TYPE.integerAttribute("quantity");
    Attribute<BigDecimal> TOTAL = TYPE.bigDecimalAttribute("total");
  }

  interface Playlist {
    EntityType<Entity> TYPE = DOMAIN.entityType("playlist@chinook");
    Attribute<Long> ID = TYPE.longAttribute("playlistid");
    Attribute<String> NAME = TYPE.stringAttribute("name");
  }

  interface PlaylistTrack {
    EntityType<Entity> TYPE = DOMAIN.entityType("playlisttrack@chinook");
    Attribute<Long> ID = TYPE.longAttribute("playlisttrackid");
    Attribute<Long> PLAYLIST_ID = TYPE.longAttribute("playlistid");
    Attribute<Entity> PLAYLIST_FK = TYPE.entityAttribute("playlist_fk");
    Attribute<Long> TRACK_ID = TYPE.longAttribute("trackid");
    Attribute<Entity> TRACK_FK = TYPE.entityAttribute("track_fk");
    Attribute<Entity> ALBUM_DENORM = TYPE.entityAttribute("album_denorm");
    Attribute<Entity> ARTIST_DENORM = TYPE.entityAttribute("artist_denorm");
  }

  static Integer getMinutes(final Integer milliseconds) {
    if (milliseconds == null) {
      return null;
    }

    return milliseconds / 1000 / 60;
  }

  static Integer getSeconds(final Integer milliseconds) {
    if (milliseconds == null) {
      return null;
    }

    return milliseconds / 1000 % 60;
  }

  static Integer getMilliseconds(final Integer minutes, final Integer seconds) {
    int milliseconds = minutes == null ? 0 : minutes * 60 * 1000;
    milliseconds += seconds == null ? 0 : seconds * 1000;

    return milliseconds == 0 ? null : milliseconds;
  }
}
