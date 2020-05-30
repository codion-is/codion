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

import static is.codion.framework.domain.property.Properties.*;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;
import static java.sql.Types.*;

public interface Chinook {

  String T_ARTIST = "artist@chinook";
  Attribute<Long> ARTIST_ARTISTID = attribute("artistid", BIGINT);
  Attribute<String> ARTIST_NAME = attribute("name", VARCHAR);
  Attribute<Integer> ARTIST_NR_OF_ALBUMS = attribute("nr_of_albums", INTEGER);
  Attribute<Integer> ARTIST_NR_OF_TRACKS = attribute("nr_of_tracks", INTEGER);

  String T_ALBUM = "album@chinook";
  Attribute<Long> ALBUM_ALBUMID = attribute("albumid", BIGINT);
  Attribute<String> ALBUM_TITLE = attribute("title", VARCHAR);
  Attribute<Long> ALBUM_ARTISTID = attribute("artistid", BIGINT);
  EntityAttribute ALBUM_ARTIST_FK = entityAttribute("artist_fk");
  BlobAttribute ALBUM_COVER = blobAttribute("cover");
  Attribute<Image> ALBUM_COVER_IMAGE = attribute("coverimage", JAVA_OBJECT);
  Attribute<Integer> ALBUM_NUMBER_OF_TRACKS = attribute("nr_of_tracks", INTEGER);

  String T_EMPLOYEE = "employee@chinook";
  Attribute<Long> EMPLOYEE_EMPLOYEEID = attribute("employeeid", BIGINT);
  Attribute<String> EMPLOYEE_LASTNAME = attribute("lastname", VARCHAR);
  Attribute<String> EMPLOYEE_FIRSTNAME = attribute("firstname", VARCHAR);
  Attribute<String> EMPLOYEE_TITLE = attribute("title", VARCHAR);
  Attribute<Long> EMPLOYEE_REPORTSTO = attribute("reportsto", BIGINT);
  EntityAttribute EMPLOYEE_REPORTSTO_FK = entityAttribute("reportsto_fk");
  Attribute<LocalDate> EMPLOYEE_BIRTHDATE = attribute("birthdate", DATE);
  Attribute<LocalDate> EMPLOYEE_HIREDATE = attribute("hiredate", DATE);
  Attribute<String> EMPLOYEE_ADDRESS = attribute("address", VARCHAR);
  Attribute<String> EMPLOYEE_CITY = attribute("city", VARCHAR);
  Attribute<String> EMPLOYEE_STATE = attribute("state", VARCHAR);
  Attribute<String> EMPLOYEE_COUNTRY = attribute("country", VARCHAR);
  Attribute<String> EMPLOYEE_POSTALCODE = attribute("postalcode", VARCHAR);
  Attribute<String> EMPLOYEE_PHONE = attribute("phone", VARCHAR);
  Attribute<String> EMPLOYEE_FAX = attribute("fax", VARCHAR);
  Attribute<String> EMPLOYEE_EMAIL = attribute("email", VARCHAR);

  String T_CUSTOMER = "customer@chinook";
  Attribute<Long> CUSTOMER_CUSTOMERID = attribute("customerid", BIGINT);
  Attribute<String> CUSTOMER_FIRSTNAME = attribute("firstname", VARCHAR);
  Attribute<String> CUSTOMER_LASTNAME = attribute("lastname", VARCHAR);
  Attribute<String> CUSTOMER_COMPANY = attribute("company", VARCHAR);
  Attribute<String> CUSTOMER_ADDRESS = attribute("address", VARCHAR);
  Attribute<String> CUSTOMER_CITY = attribute("city", VARCHAR);
  Attribute<String> CUSTOMER_STATE = attribute("state", VARCHAR);
  Attribute<String> CUSTOMER_COUNTRY = attribute("country", VARCHAR);
  Attribute<String> CUSTOMER_POSTALCODE = attribute("postalcode", VARCHAR);
  Attribute<String> CUSTOMER_PHONE = attribute("phone", VARCHAR);
  Attribute<String> CUSTOMER_FAX = attribute("fax", VARCHAR);
  Attribute<String> CUSTOMER_EMAIL = attribute("email", VARCHAR);
  Attribute<String> CUSTOMER_SUPPORTREPID = attribute("supportrepid", BIGINT);
  EntityAttribute CUSTOMER_SUPPORTREP_FK = entityAttribute("supportrep_fk");

  JasperReportWrapper CUSTOMER_REPORT = classPathReport(Chinook.class, "customer_report.jasper");

  String T_GENRE = "genre@chinook";
  Attribute<Long> GENRE_GENREID = attribute("genreid", BIGINT);
  Attribute<String> GENRE_NAME = attribute("name", VARCHAR);

  String T_MEDIATYPE = "mediatype@chinook";
  Attribute<Long> MEDIATYPE_MEDIATYPEID = attribute("mediatypeid", BIGINT);
  Attribute<String> MEDIATYPE_NAME = attribute("name", VARCHAR);

  String T_TRACK = "track@chinook";
  Attribute<Long> TRACK_TRACKID = attribute("trackid", BIGINT);
  Attribute<String> TRACK_NAME = attribute("name", VARCHAR);
  EntityAttribute TRACK_ARTIST_DENORM = entityAttribute("artist_denorm");
  Attribute<Long> TRACK_ALBUMID = attribute("albumid", BIGINT);
  EntityAttribute TRACK_ALBUM_FK = entityAttribute("album_fk");
  Attribute<Long> TRACK_MEDIATYPEID = attribute("mediatypeid", BIGINT);
  EntityAttribute TRACK_MEDIATYPE_FK = entityAttribute("mediatype_fk");
  Attribute<Long> TRACK_GENREID = attribute("genreid", BIGINT);
  EntityAttribute TRACK_GENRE_FK = entityAttribute("genre_fk");
  Attribute<String> TRACK_COMPOSER = attribute("composer", VARCHAR);
  Attribute<Integer> TRACK_MILLISECONDS = attribute("milliseconds", INTEGER);
  Attribute<String> TRACK_MINUTES_SECONDS_DERIVED = attribute("minutes_seconds_transient", VARCHAR);
  Attribute<Integer> TRACK_BYTES = attribute("bytes", INTEGER);
  Attribute<BigDecimal> TRACK_UNITPRICE = attribute("unitprice", DECIMAL);

  DerivedProperty.Provider TRACK_MIN_SEC_PROVIDER =
          linkedValues -> {
            final Integer milliseconds = (Integer) linkedValues.get(TRACK_MILLISECONDS);
            if (milliseconds == null || milliseconds <= 0) {
              return "";
            }

            return getMinutes(milliseconds) + " min " + getSeconds(milliseconds) + " sec";
          };

  String T_INVOICE = "invoice@chinook";
  Attribute<Long> INVOICE_INVOICEID = attribute("invoiceid", BIGINT);
  Attribute<Long> INVOICE_CUSTOMERID = attribute("customerid", BIGINT);
  EntityAttribute INVOICE_CUSTOMER_FK = entityAttribute("customer_fk");
  Attribute<LocalDateTime> INVOICE_INVOICEDATE = attribute("invoicedate", TIMESTAMP);
  Attribute<String> INVOICE_BILLINGADDRESS = attribute("billingaddress", VARCHAR);
  Attribute<String> INVOICE_BILLINGCITY = attribute("billingcity", VARCHAR);
  Attribute<String> INVOICE_BILLINGSTATE = attribute("billingstate", VARCHAR);
  Attribute<String> INVOICE_BILLINGCOUNTRY = attribute("billingcountry", VARCHAR);
  Attribute<String> INVOICE_BILLINGPOSTALCODE = attribute("billingpostalcode", VARCHAR);
  Attribute<BigDecimal> INVOICE_TOTAL = attribute("total", DECIMAL);
  Attribute<BigDecimal> INVOICE_TOTAL_SUB = attribute("total_sub", DECIMAL);

  String T_INVOICELINE = "invoiceline@chinook";
  Attribute<Long> INVOICELINE_INVOICELINEID = attribute("invoicelineid", BIGINT);
  Attribute<Long> INVOICELINE_INVOICEID = attribute("invoiceid", BIGINT);
  EntityAttribute INVOICELINE_INVOICE_FK = entityAttribute("invoice_fk");
  Attribute<Long> INVOICELINE_TRACKID = attribute("trackid", BIGINT);
  EntityAttribute INVOICELINE_TRACK_FK = entityAttribute("track_fk");
  Attribute<BigDecimal> INVOICELINE_UNITPRICE = attribute("unitprice", DECIMAL);
  Attribute<Integer> INVOICELINE_QUANTITY = attribute("quantity", INTEGER);
  Attribute<Double> INVOICELINE_TOTAL = attribute("total", DOUBLE);

  DerivedProperty.Provider INVOICELINE_TOTAL_PROVIDER =
          linkedValues -> {
            final Integer quantity = (Integer) linkedValues.get(INVOICELINE_QUANTITY);
            final BigDecimal unitPrice = (BigDecimal) linkedValues.get(INVOICELINE_UNITPRICE);
            if (unitPrice == null || quantity == null) {
              return null;
            }

            return unitPrice.multiply(BigDecimal.valueOf(quantity));
          };

  String T_PLAYLIST = "playlist@chinook";
  Attribute<Long> PLAYLIST_PLAYLISTID = attribute("playlistid", BIGINT);
  Attribute<String> PLAYLIST_NAME = attribute("name", VARCHAR);

  String T_PLAYLISTTRACK = "playlisttrack@chinook";
  Attribute<Long> PLAYLISTTRACK_ID = attribute("playlisttrackid", BIGINT);
  Attribute<Long> PLAYLISTTRACK_PLAYLISTID = attribute("playlistid", BIGINT);
  EntityAttribute PLAYLISTTRACK_PLAYLIST_FK = entityAttribute("playlist_fk");
  Attribute<Long> PLAYLISTTRACK_TRACKID = attribute("trackid", BIGINT);
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
