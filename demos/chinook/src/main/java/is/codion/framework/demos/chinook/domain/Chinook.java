/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.awt.Image;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static is.codion.framework.domain.property.Properties.attribute;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;

public interface Chinook {

  String T_ARTIST = "artist@chinook";
  Attribute<Long> ARTIST_ARTISTID = attribute("artistid");
  Attribute<String> ARTIST_NAME = attribute("name");
  Attribute<Integer> ARTIST_NR_OF_ALBUMS = attribute("nr_of_albums");
  Attribute<Integer> ARTIST_NR_OF_TRACKS = attribute("nr_of_tracks");

  String T_ALBUM = "album@chinook";
  Attribute<Long> ALBUM_ALBUMID = attribute("albumid");
  Attribute<String> ALBUM_TITLE = attribute("title");
  Attribute<Long> ALBUM_ARTISTID = attribute("artistid");
  Attribute<Entity> ALBUM_ARTIST_FK = attribute("artist_fk");
  Attribute<byte[]> ALBUM_COVER = attribute("cover");
  Attribute<Image> ALBUM_COVER_IMAGE = attribute("coverimage");
  Attribute<Integer> ALBUM_NUMBER_OF_TRACKS = attribute("nr_of_tracks");

  String T_EMPLOYEE = "employee@chinook";
  Attribute<Long> EMPLOYEE_EMPLOYEEID = attribute("employeeid");
  Attribute<String> EMPLOYEE_LASTNAME = attribute("lastname");
  Attribute<String> EMPLOYEE_FIRSTNAME = attribute("firstname");
  Attribute<String> EMPLOYEE_TITLE = attribute("title");
  Attribute<Long> EMPLOYEE_REPORTSTO = attribute("reportsto");
  Attribute<Entity> EMPLOYEE_REPORTSTO_FK = attribute("reportsto_fk");
  Attribute<LocalDate> EMPLOYEE_BIRTHDATE = attribute("birthdate");
  Attribute<LocalDate> EMPLOYEE_HIREDATE = attribute("hiredate");
  Attribute<String> EMPLOYEE_ADDRESS = attribute("address");
  Attribute<String> EMPLOYEE_CITY = attribute("city");
  Attribute<String> EMPLOYEE_STATE = attribute("state");
  Attribute<String> EMPLOYEE_COUNTRY = attribute("country");
  Attribute<String> EMPLOYEE_POSTALCODE = attribute("postalcode");
  Attribute<String> EMPLOYEE_PHONE = attribute("phone");
  Attribute<String> EMPLOYEE_FAX = attribute("fax");
  Attribute<String> EMPLOYEE_EMAIL = attribute("email");

  String T_CUSTOMER = "customer@chinook";
  Attribute<Long> CUSTOMER_CUSTOMERID = attribute("customerid");
  Attribute<String> CUSTOMER_FIRSTNAME = attribute("firstname");
  Attribute<String> CUSTOMER_LASTNAME = attribute("lastname");
  Attribute<String> CUSTOMER_COMPANY = attribute("company");
  Attribute<String> CUSTOMER_ADDRESS = attribute("address");
  Attribute<String> CUSTOMER_CITY = attribute("city");
  Attribute<String> CUSTOMER_STATE = attribute("state");
  Attribute<String> CUSTOMER_COUNTRY = attribute("country");
  Attribute<String> CUSTOMER_POSTALCODE = attribute("postalcode");
  Attribute<String> CUSTOMER_PHONE = attribute("phone");
  Attribute<String> CUSTOMER_FAX = attribute("fax");
  Attribute<String> CUSTOMER_EMAIL = attribute("email");
  Attribute<String> CUSTOMER_SUPPORTREPID = attribute("supportrepid");
  Attribute<Entity> CUSTOMER_SUPPORTREP_FK = attribute("supportrep_fk");

  JasperReportWrapper CUSTOMER_REPORT = classPathReport(Chinook.class, "customer_report.jasper");

  String T_GENRE = "genre@chinook";
  Attribute<Long> GENRE_GENREID = attribute("genreid");
  Attribute<String> GENRE_NAME = attribute("name");

  String T_MEDIATYPE = "mediatype@chinook";
  Attribute<Long> MEDIATYPE_MEDIATYPEID = attribute("mediatypeid");
  Attribute<String> MEDIATYPE_NAME = attribute("name");

  String T_TRACK = "track@chinook";
  Attribute<Long> TRACK_TRACKID = attribute("trackid");
  Attribute<String> TRACK_NAME = attribute("name");
  Attribute<Entity> TRACK_ARTIST_DENORM = attribute("artist_denorm");
  Attribute<Long> TRACK_ALBUMID = attribute("albumid");
  Attribute<Entity> TRACK_ALBUM_FK = attribute("album_fk");
  Attribute<Long> TRACK_MEDIATYPEID = attribute("mediatypeid");
  Attribute<Entity> TRACK_MEDIATYPE_FK = attribute("mediatype_fk");
  Attribute<Long> TRACK_GENREID = attribute("genreid");
  Attribute<Entity> TRACK_GENRE_FK = attribute("genre_fk");
  Attribute<String> TRACK_COMPOSER = attribute("composer");
  Attribute<Integer> TRACK_MILLISECONDS = attribute("milliseconds");
  Attribute<String> TRACK_MINUTES_SECONDS_DERIVED = attribute("minutes_seconds_transient");
  Attribute<Integer> TRACK_BYTES = attribute("bytes");
  Attribute<BigDecimal> TRACK_UNITPRICE = attribute("unitprice");

  DerivedProperty.Provider TRACK_MIN_SEC_PROVIDER =
          linkedValues -> {
            final Integer milliseconds = (Integer) linkedValues.get(TRACK_MILLISECONDS);
            if (milliseconds == null || milliseconds <= 0) {
              return "";
            }

            return getMinutes(milliseconds) + " min " + getSeconds(milliseconds) + " sec";
          };

  String T_INVOICE = "invoice@chinook";
  Attribute<Long> INVOICE_INVOICEID = attribute("invoiceid");
  Attribute<Long> INVOICE_CUSTOMERID = attribute("customerid");
  Attribute<Entity> INVOICE_CUSTOMER_FK = attribute("customer_fk");
  Attribute<LocalDateTime> INVOICE_INVOICEDATE = attribute("invoicedate");
  Attribute<String> INVOICE_BILLINGADDRESS = attribute("billingaddress");
  Attribute<String> INVOICE_BILLINGCITY = attribute("billingcity");
  Attribute<String> INVOICE_BILLINGSTATE = attribute("billingstate");
  Attribute<String> INVOICE_BILLINGCOUNTRY = attribute("billingcountry");
  Attribute<String> INVOICE_BILLINGPOSTALCODE = attribute("billingpostalcode");
  Attribute<BigDecimal> INVOICE_TOTAL = attribute("total");
  Attribute<BigDecimal> INVOICE_TOTAL_SUB = attribute("total_sub");

  String T_INVOICELINE = "invoiceline@chinook";
  Attribute<Long> INVOICELINE_INVOICELINEID = attribute("invoicelineid");
  Attribute<Long> INVOICELINE_INVOICEID = attribute("invoiceid");
  Attribute<Entity> INVOICELINE_INVOICE_FK = attribute("invoice_fk");
  Attribute<Long> INVOICELINE_TRACKID = attribute("trackid");
  Attribute<Entity> INVOICELINE_TRACK_FK = attribute("track_fk");
  Attribute<BigDecimal> INVOICELINE_UNITPRICE = attribute("unitprice");
  Attribute<Integer> INVOICELINE_QUANTITY = attribute("quantity");
  Attribute<Double> INVOICELINE_TOTAL = attribute("total");

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
  Attribute<Long> PLAYLIST_PLAYLISTID = attribute("playlistid");
  Attribute<String> PLAYLIST_NAME = attribute("name");

  String T_PLAYLISTTRACK = "playlisttrack@chinook";
  Attribute<Long> PLAYLISTTRACK_ID = attribute("playlisttrackid");
  Attribute<Long> PLAYLISTTRACK_PLAYLISTID = attribute("playlistid");
  Attribute<Entity> PLAYLISTTRACK_PLAYLIST_FK = attribute("playlist_fk");
  Attribute<Long> PLAYLISTTRACK_TRACKID = attribute("trackid");
  Attribute<Entity> PLAYLISTTRACK_TRACK_FK = attribute("track_fk");
  Attribute<Entity> PLAYLISTTRACK_ALBUM_DENORM = attribute("album_denorm");
  Attribute<Entity> PLAYLISTTRACK_ARTIST_DENORM = attribute("artist_denorm");

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
