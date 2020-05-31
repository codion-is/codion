/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain;

import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.BlobAttribute;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.EntityAttribute;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.awt.Image;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static is.codion.framework.domain.property.Attributes.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;

public interface Chinook {

  String T_ARTIST = "artist@chinook";
  Attribute<Long> ARTIST_ARTISTID = longAttribute("artistid", T_ARTIST);
  Attribute<String> ARTIST_NAME = stringAttribute("name", T_ARTIST);
  Attribute<Integer> ARTIST_NR_OF_ALBUMS = integerAttribute("nr_of_albums", T_ARTIST);
  Attribute<Integer> ARTIST_NR_OF_TRACKS = integerAttribute("nr_of_tracks", T_ARTIST);

  String T_ALBUM = "album@chinook";
  Attribute<Long> ALBUM_ALBUMID = longAttribute("albumid", T_ALBUM);
  Attribute<String> ALBUM_TITLE = stringAttribute("title", T_ALBUM);
  Attribute<Long> ALBUM_ARTISTID = longAttribute("artistid", T_ALBUM);
  EntityAttribute ALBUM_ARTIST_FK = entityAttribute("artist_fk", T_ALBUM);
  BlobAttribute ALBUM_COVER = blobAttribute("cover", T_ALBUM);
  Attribute<Image> ALBUM_COVER_IMAGE = attribute("coverimage", Image.class, T_ALBUM);
  Attribute<Integer> ALBUM_NUMBER_OF_TRACKS = integerAttribute("nr_of_tracks", T_ALBUM);

  String T_EMPLOYEE = "employee@chinook";
  Attribute<Long> EMPLOYEE_EMPLOYEEID = longAttribute("employeeid", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_LASTNAME = stringAttribute("lastname", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_FIRSTNAME = stringAttribute("firstname", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_TITLE = stringAttribute("title", T_EMPLOYEE);
  Attribute<Long> EMPLOYEE_REPORTSTO = longAttribute("reportsto", T_EMPLOYEE);
  EntityAttribute EMPLOYEE_REPORTSTO_FK = entityAttribute("reportsto_fk", T_EMPLOYEE);
  Attribute<LocalDate> EMPLOYEE_BIRTHDATE = localDateAttribute("birthdate", T_EMPLOYEE);
  Attribute<LocalDate> EMPLOYEE_HIREDATE = localDateAttribute("hiredate", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_ADDRESS = stringAttribute("address", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_CITY = stringAttribute("city", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_STATE = stringAttribute("state", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_COUNTRY = stringAttribute("country", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_POSTALCODE = stringAttribute("postalcode", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_PHONE = stringAttribute("phone", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_FAX = stringAttribute("fax", T_EMPLOYEE);
  Attribute<String> EMPLOYEE_EMAIL = stringAttribute("email", T_EMPLOYEE);

  String T_CUSTOMER = "customer@chinook";
  Attribute<Long> CUSTOMER_CUSTOMERID = longAttribute("customerid", T_CUSTOMER);
  Attribute<String> CUSTOMER_FIRSTNAME = stringAttribute("firstname", T_CUSTOMER);
  Attribute<String> CUSTOMER_LASTNAME = stringAttribute("lastname", T_CUSTOMER);
  Attribute<String> CUSTOMER_COMPANY = stringAttribute("company", T_CUSTOMER);
  Attribute<String> CUSTOMER_ADDRESS = stringAttribute("address", T_CUSTOMER);
  Attribute<String> CUSTOMER_CITY = stringAttribute("city", T_CUSTOMER);
  Attribute<String> CUSTOMER_STATE = stringAttribute("state", T_CUSTOMER);
  Attribute<String> CUSTOMER_COUNTRY = stringAttribute("country", T_CUSTOMER);
  Attribute<String> CUSTOMER_POSTALCODE = stringAttribute("postalcode", T_CUSTOMER);
  Attribute<String> CUSTOMER_PHONE = stringAttribute("phone", T_CUSTOMER);
  Attribute<String> CUSTOMER_FAX = stringAttribute("fax", T_CUSTOMER);
  Attribute<String> CUSTOMER_EMAIL = stringAttribute("email", T_CUSTOMER);
  Attribute<Long> CUSTOMER_SUPPORTREPID = longAttribute("supportrepid", T_CUSTOMER);
  EntityAttribute CUSTOMER_SUPPORTREP_FK = entityAttribute("supportrep_fk", T_CUSTOMER);

  JasperReportWrapper CUSTOMER_REPORT = classPathReport(Chinook.class, "customer_report.jasper");

  String T_GENRE = "genre@chinook";
  Attribute<Long> GENRE_GENREID = longAttribute("genreid", T_GENRE);
  Attribute<String> GENRE_NAME = stringAttribute("name", T_GENRE);

  String T_MEDIATYPE = "mediatype@chinook";
  Attribute<Long> MEDIATYPE_MEDIATYPEID = longAttribute("mediatypeid", T_MEDIATYPE);
  Attribute<String> MEDIATYPE_NAME = stringAttribute("name", T_MEDIATYPE);

  String T_TRACK = "track@chinook";
  Attribute<Long> TRACK_TRACKID = longAttribute("trackid", T_TRACK);
  Attribute<String> TRACK_NAME = stringAttribute("name", T_TRACK);
  EntityAttribute TRACK_ARTIST_DENORM = entityAttribute("artist_denorm", T_TRACK);
  Attribute<Long> TRACK_ALBUMID = longAttribute("albumid", T_TRACK);
  EntityAttribute TRACK_ALBUM_FK = entityAttribute("album_fk", T_TRACK);
  Attribute<Long> TRACK_MEDIATYPEID = longAttribute("mediatypeid", T_TRACK);
  EntityAttribute TRACK_MEDIATYPE_FK = entityAttribute("mediatype_fk", T_TRACK);
  Attribute<Long> TRACK_GENREID = longAttribute("genreid", T_TRACK);
  EntityAttribute TRACK_GENRE_FK = entityAttribute("genre_fk", T_TRACK);
  Attribute<String> TRACK_COMPOSER = stringAttribute("composer", T_TRACK);
  Attribute<Integer> TRACK_MILLISECONDS = integerAttribute("milliseconds", T_TRACK);
  Attribute<String> TRACK_MINUTES_SECONDS_DERIVED = stringAttribute("minutes_seconds_transient", T_TRACK);
  Attribute<Integer> TRACK_BYTES = integerAttribute("bytes", T_TRACK);
  Attribute<BigDecimal> TRACK_UNITPRICE = bigDecimalAttribute("unitprice", T_TRACK);

  DerivedProperty.Provider<String> TRACK_MIN_SEC_PROVIDER =
          linkedValues -> {
            final Integer milliseconds = (Integer) linkedValues.get(TRACK_MILLISECONDS);
            if (milliseconds == null || milliseconds <= 0) {
              return "";
            }

            return getMinutes(milliseconds) + " min " + getSeconds(milliseconds) + " sec";
          };

  String T_INVOICE = "invoice@chinook";
  Attribute<Long> INVOICE_INVOICEID = longAttribute("invoiceid", T_INVOICE);
  Attribute<Long> INVOICE_CUSTOMERID = longAttribute("customerid", T_INVOICE);
  EntityAttribute INVOICE_CUSTOMER_FK = entityAttribute("customer_fk", T_INVOICE);
  Attribute<LocalDateTime> INVOICE_INVOICEDATE = localDateTimeAttribute("invoicedate", T_INVOICE);
  Attribute<String> INVOICE_BILLINGADDRESS = stringAttribute("billingaddress", T_INVOICE);
  Attribute<String> INVOICE_BILLINGCITY = stringAttribute("billingcity", T_INVOICE);
  Attribute<String> INVOICE_BILLINGSTATE = stringAttribute("billingstate", T_INVOICE);
  Attribute<String> INVOICE_BILLINGCOUNTRY = stringAttribute("billingcountry", T_INVOICE);
  Attribute<String> INVOICE_BILLINGPOSTALCODE = stringAttribute("billingpostalcode", T_INVOICE);
  Attribute<BigDecimal> INVOICE_TOTAL = bigDecimalAttribute("total", T_INVOICE);
  Attribute<BigDecimal> INVOICE_TOTAL_SUB = bigDecimalAttribute("total_sub", T_INVOICE);

  String T_INVOICELINE = "invoiceline@chinook";
  Attribute<Long> INVOICELINE_INVOICELINEID = longAttribute("invoicelineid", T_INVOICELINE);
  Attribute<Long> INVOICELINE_INVOICEID = longAttribute("invoiceid", T_INVOICELINE);
  EntityAttribute INVOICELINE_INVOICE_FK = entityAttribute("invoice_fk", T_INVOICELINE);
  Attribute<Long> INVOICELINE_TRACKID = longAttribute("trackid", T_INVOICELINE);
  EntityAttribute INVOICELINE_TRACK_FK = entityAttribute("track_fk", T_INVOICELINE);
  Attribute<BigDecimal> INVOICELINE_UNITPRICE = bigDecimalAttribute("unitprice", T_INVOICELINE);
  Attribute<Integer> INVOICELINE_QUANTITY = integerAttribute("quantity", T_INVOICELINE);
  Attribute<BigDecimal> INVOICELINE_TOTAL = bigDecimalAttribute("total", T_INVOICELINE);

  DerivedProperty.Provider<BigDecimal> INVOICELINE_TOTAL_PROVIDER =
          linkedValues -> {
            final Integer quantity = (Integer) linkedValues.get(INVOICELINE_QUANTITY);
            final BigDecimal unitPrice = (BigDecimal) linkedValues.get(INVOICELINE_UNITPRICE);
            if (unitPrice == null || quantity == null) {
              return null;
            }

            return unitPrice.multiply(BigDecimal.valueOf(quantity));
          };

  String T_PLAYLIST = "playlist@chinook";
  Attribute<Long> PLAYLIST_PLAYLISTID = longAttribute("playlistid", T_PLAYLIST);
  Attribute<String> PLAYLIST_NAME = stringAttribute("name", T_PLAYLIST);

  String T_PLAYLISTTRACK = "playlisttrack@chinook";
  Attribute<Long> PLAYLISTTRACK_ID = longAttribute("playlisttrackid", T_PLAYLISTTRACK);
  Attribute<Long> PLAYLISTTRACK_PLAYLISTID = longAttribute("playlistid", T_PLAYLISTTRACK);
  EntityAttribute PLAYLISTTRACK_PLAYLIST_FK = entityAttribute("playlist_fk", T_PLAYLISTTRACK);
  Attribute<Long> PLAYLISTTRACK_TRACKID = longAttribute("trackid", T_PLAYLISTTRACK);
  EntityAttribute PLAYLISTTRACK_TRACK_FK = entityAttribute("track_fk", T_PLAYLISTTRACK);
  EntityAttribute PLAYLISTTRACK_ALBUM_DENORM = entityAttribute("album_denorm", T_PLAYLISTTRACK);
  EntityAttribute PLAYLISTTRACK_ARTIST_DENORM = entityAttribute("artist_denorm", T_PLAYLISTTRACK);

  String P_UPDATE_TOTALS = "chinook.update_totals_procedure";
  String F_RAISE_PRICE = "chinook.raise_price_function";

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
