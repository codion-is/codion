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
  Attribute<Long> ARTIST_ARTISTID = longAttribute("artistid");
  Attribute<String> ARTIST_NAME = stringAttribute("name");
  Attribute<Integer> ARTIST_NR_OF_ALBUMS = integerAttribute("nr_of_albums");
  Attribute<Integer> ARTIST_NR_OF_TRACKS = integerAttribute("nr_of_tracks");

  String T_ALBUM = "album@chinook";
  Attribute<Long> ALBUM_ALBUMID = longAttribute("albumid");
  Attribute<String> ALBUM_TITLE = stringAttribute("title");
  Attribute<Long> ALBUM_ARTISTID = longAttribute("artistid");
  EntityAttribute ALBUM_ARTIST_FK = entityAttribute("artist_fk");
  BlobAttribute ALBUM_COVER = blobAttribute("cover");
  Attribute<Image> ALBUM_COVER_IMAGE = attribute("coverimage", Image.class);
  Attribute<Integer> ALBUM_NUMBER_OF_TRACKS = integerAttribute("nr_of_tracks");

  String T_EMPLOYEE = "employee@chinook";
  Attribute<Long> EMPLOYEE_EMPLOYEEID = longAttribute("employeeid");
  Attribute<String> EMPLOYEE_LASTNAME = stringAttribute("lastname");
  Attribute<String> EMPLOYEE_FIRSTNAME = stringAttribute("firstname");
  Attribute<String> EMPLOYEE_TITLE = stringAttribute("title");
  Attribute<Long> EMPLOYEE_REPORTSTO = longAttribute("reportsto");
  EntityAttribute EMPLOYEE_REPORTSTO_FK = entityAttribute("reportsto_fk");
  Attribute<LocalDate> EMPLOYEE_BIRTHDATE = localDateAttribute("birthdate");
  Attribute<LocalDate> EMPLOYEE_HIREDATE = localDateAttribute("hiredate");
  Attribute<String> EMPLOYEE_ADDRESS = stringAttribute("address");
  Attribute<String> EMPLOYEE_CITY = stringAttribute("city");
  Attribute<String> EMPLOYEE_STATE = stringAttribute("state");
  Attribute<String> EMPLOYEE_COUNTRY = stringAttribute("country");
  Attribute<String> EMPLOYEE_POSTALCODE = stringAttribute("postalcode");
  Attribute<String> EMPLOYEE_PHONE = stringAttribute("phone");
  Attribute<String> EMPLOYEE_FAX = stringAttribute("fax");
  Attribute<String> EMPLOYEE_EMAIL = stringAttribute("email");

  String T_CUSTOMER = "customer@chinook";
  Attribute<Long> CUSTOMER_CUSTOMERID = longAttribute("customerid");
  Attribute<String> CUSTOMER_FIRSTNAME = stringAttribute("firstname");
  Attribute<String> CUSTOMER_LASTNAME = stringAttribute("lastname");
  Attribute<String> CUSTOMER_COMPANY = stringAttribute("company");
  Attribute<String> CUSTOMER_ADDRESS = stringAttribute("address");
  Attribute<String> CUSTOMER_CITY = stringAttribute("city");
  Attribute<String> CUSTOMER_STATE = stringAttribute("state");
  Attribute<String> CUSTOMER_COUNTRY = stringAttribute("country");
  Attribute<String> CUSTOMER_POSTALCODE = stringAttribute("postalcode");
  Attribute<String> CUSTOMER_PHONE = stringAttribute("phone");
  Attribute<String> CUSTOMER_FAX = stringAttribute("fax");
  Attribute<String> CUSTOMER_EMAIL = stringAttribute("email");
  Attribute<Long> CUSTOMER_SUPPORTREPID = longAttribute("supportrepid");
  EntityAttribute CUSTOMER_SUPPORTREP_FK = entityAttribute("supportrep_fk");

  JasperReportWrapper CUSTOMER_REPORT = classPathReport(Chinook.class, "customer_report.jasper");

  String T_GENRE = "genre@chinook";
  Attribute<Long> GENRE_GENREID = longAttribute("genreid");
  Attribute<String> GENRE_NAME = stringAttribute("name");

  String T_MEDIATYPE = "mediatype@chinook";
  Attribute<Long> MEDIATYPE_MEDIATYPEID = longAttribute("mediatypeid");
  Attribute<String> MEDIATYPE_NAME = stringAttribute("name");

  String T_TRACK = "track@chinook";
  Attribute<Long> TRACK_TRACKID = longAttribute("trackid");
  Attribute<String> TRACK_NAME = stringAttribute("name");
  EntityAttribute TRACK_ARTIST_DENORM = entityAttribute("artist_denorm");
  Attribute<Long> TRACK_ALBUMID = longAttribute("albumid");
  EntityAttribute TRACK_ALBUM_FK = entityAttribute("album_fk");
  Attribute<Long> TRACK_MEDIATYPEID = longAttribute("mediatypeid");
  EntityAttribute TRACK_MEDIATYPE_FK = entityAttribute("mediatype_fk");
  Attribute<Long> TRACK_GENREID = longAttribute("genreid");
  EntityAttribute TRACK_GENRE_FK = entityAttribute("genre_fk");
  Attribute<String> TRACK_COMPOSER = stringAttribute("composer");
  Attribute<Integer> TRACK_MILLISECONDS = integerAttribute("milliseconds");
  Attribute<String> TRACK_MINUTES_SECONDS_DERIVED = stringAttribute("minutes_seconds_transient");
  Attribute<Integer> TRACK_BYTES = integerAttribute("bytes");
  Attribute<BigDecimal> TRACK_UNITPRICE = bigDecimalAttribute("unitprice");

  DerivedProperty.Provider<String> TRACK_MIN_SEC_PROVIDER =
          linkedValues -> {
            final Integer milliseconds = (Integer) linkedValues.get(TRACK_MILLISECONDS);
            if (milliseconds == null || milliseconds <= 0) {
              return "";
            }

            return getMinutes(milliseconds) + " min " + getSeconds(milliseconds) + " sec";
          };

  String T_INVOICE = "invoice@chinook";
  Attribute<Long> INVOICE_INVOICEID = longAttribute("invoiceid");
  Attribute<Long> INVOICE_CUSTOMERID = longAttribute("customerid");
  EntityAttribute INVOICE_CUSTOMER_FK = entityAttribute("customer_fk");
  Attribute<LocalDateTime> INVOICE_INVOICEDATE = localDateTimeAttribute("invoicedate");
  Attribute<String> INVOICE_BILLINGADDRESS = stringAttribute("billingaddress");
  Attribute<String> INVOICE_BILLINGCITY = stringAttribute("billingcity");
  Attribute<String> INVOICE_BILLINGSTATE = stringAttribute("billingstate");
  Attribute<String> INVOICE_BILLINGCOUNTRY = stringAttribute("billingcountry");
  Attribute<String> INVOICE_BILLINGPOSTALCODE = stringAttribute("billingpostalcode");
  Attribute<BigDecimal> INVOICE_TOTAL = bigDecimalAttribute("total");
  Attribute<BigDecimal> INVOICE_TOTAL_SUB = bigDecimalAttribute("total_sub");

  String T_INVOICELINE = "invoiceline@chinook";
  Attribute<Long> INVOICELINE_INVOICELINEID = longAttribute("invoicelineid");
  Attribute<Long> INVOICELINE_INVOICEID = longAttribute("invoiceid");
  EntityAttribute INVOICELINE_INVOICE_FK = entityAttribute("invoice_fk");
  Attribute<Long> INVOICELINE_TRACKID = longAttribute("trackid");
  EntityAttribute INVOICELINE_TRACK_FK = entityAttribute("track_fk");
  Attribute<BigDecimal> INVOICELINE_UNITPRICE = bigDecimalAttribute("unitprice");
  Attribute<Integer> INVOICELINE_QUANTITY = integerAttribute("quantity");
  Attribute<BigDecimal> INVOICELINE_TOTAL = bigDecimalAttribute("total");

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
  Attribute<Long> PLAYLIST_PLAYLISTID = longAttribute("playlistid");
  Attribute<String> PLAYLIST_NAME = stringAttribute("name");

  String T_PLAYLISTTRACK = "playlisttrack@chinook";
  Attribute<Long> PLAYLISTTRACK_ID = longAttribute("playlisttrackid");
  Attribute<Long> PLAYLISTTRACK_PLAYLISTID = longAttribute("playlistid");
  EntityAttribute PLAYLISTTRACK_PLAYLIST_FK = entityAttribute("playlist_fk");
  Attribute<Long> PLAYLISTTRACK_TRACKID = longAttribute("trackid");
  EntityAttribute PLAYLISTTRACK_TRACK_FK = entityAttribute("track_fk");
  EntityAttribute PLAYLISTTRACK_ALBUM_DENORM = entityAttribute("album_denorm");
  EntityAttribute PLAYLISTTRACK_ARTIST_DENORM = entityAttribute("artist_denorm");

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
