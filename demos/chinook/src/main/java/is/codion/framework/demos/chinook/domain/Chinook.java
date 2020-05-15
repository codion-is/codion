/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain;

import is.codion.framework.domain.property.DerivedProperty;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.math.BigDecimal;

import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;

public interface Chinook {

  String T_ARTIST = "artist@chinook";
  String ARTIST_ARTISTID = "artistid";
  String ARTIST_NAME = "name";
  String ARTIST_NR_OF_ALBUMS = "nr_of_albums";
  String ARTIST_NR_OF_TRACKS = "nr_of_tracks";

  String T_ALBUM = "album@chinook";
  String ALBUM_ALBUMID = "albumid";
  String ALBUM_TITLE = "title";
  String ALBUM_ARTISTID = "artistid";
  String ALBUM_ARTIST_FK = "artist_fk";
  String ALBUM_COVER = "cover";
  String ALBUM_COVER_IMAGE = "coverimage";
  String ALBUM_NUMBER_OF_TRACKS = "nr_of_tracks";

  String T_EMPLOYEE = "employee@chinook";
  String EMPLOYEE_EMPLOYEEID = "employeeid";
  String EMPLOYEE_LASTNAME = "lastname";
  String EMPLOYEE_FIRSTNAME = "firstname";
  String EMPLOYEE_TITLE = "title";
  String EMPLOYEE_REPORTSTO = "reportsto";
  String EMPLOYEE_REPORTSTO_FK = "reportsto_fk";
  String EMPLOYEE_BIRTHDATE = "birthdate";
  String EMPLOYEE_HIREDATE = "hiredate";
  String EMPLOYEE_ADDRESS = "address";
  String EMPLOYEE_CITY = "city";
  String EMPLOYEE_STATE = "state";
  String EMPLOYEE_COUNTRY = "country";
  String EMPLOYEE_POSTALCODE = "postalcode";
  String EMPLOYEE_PHONE = "phone";
  String EMPLOYEE_FAX = "fax";
  String EMPLOYEE_EMAIL = "email";

  String T_CUSTOMER = "customer@chinook";
  String CUSTOMER_CUSTOMERID = "customerid";
  String CUSTOMER_FIRSTNAME = "firstname";
  String CUSTOMER_LASTNAME = "lastname";
  String CUSTOMER_COMPANY = "company";
  String CUSTOMER_ADDRESS = "address";
  String CUSTOMER_CITY = "city";
  String CUSTOMER_STATE = "state";
  String CUSTOMER_COUNTRY = "country";
  String CUSTOMER_POSTALCODE = "postalcode";
  String CUSTOMER_PHONE = "phone";
  String CUSTOMER_FAX = "fax";
  String CUSTOMER_EMAIL = "email";
  String CUSTOMER_SUPPORTREPID = "supportrepid";
  String CUSTOMER_SUPPORTREP_FK = "supportrep_fk";

  JasperReportWrapper CUSTOMER_REPORT = classPathReport(Chinook.class, "customer_report.jasper");

  String T_GENRE = "genre@chinook";
  String GENRE_GENREID = "genreid";
  String GENRE_NAME = "name";

  String T_MEDIATYPE = "mediatype@chinook";
  String MEDIATYPE_MEDIATYPEID = "mediatypeid";
  String MEDIATYPE_NAME = "name";

  String T_TRACK = "track@chinook";
  String TRACK_TRACKID = "trackid";
  String TRACK_NAME = "name";
  String TRACK_ARTIST_DENORM = "artist_denorm";
  String TRACK_ALBUMID = "albumid";
  String TRACK_ALBUM_FK = "album_fk";
  String TRACK_MEDIATYPEID = "mediatypeid";
  String TRACK_MEDIATYPE_FK = "mediatype_fk";
  String TRACK_GENREID = "genreid";
  String TRACK_GENRE_FK = "genre_fk";
  String TRACK_COMPOSER = "composer";
  String TRACK_MILLISECONDS = "milliseconds";
  String TRACK_MINUTES_SECONDS_DERIVED = "minutes_seconds_transient";
  String TRACK_BYTES = "bytes";
  String TRACK_UNITPRICE = "unitprice";

  DerivedProperty.Provider TRACK_MIN_SEC_PROVIDER =
          linkedValues -> {
            final Integer milliseconds = (Integer) linkedValues.get(TRACK_MILLISECONDS);
            if (milliseconds == null || milliseconds <= 0) {
              return "";
            }

            return getMinutes(milliseconds) + " min " + getSeconds(milliseconds) + " sec";
          };

  String T_INVOICE = "invoice@chinook";
  String INVOICE_INVOICEID = "invoiceid";
  String INVOICE_CUSTOMERID = "customerid";
  String INVOICE_CUSTOMER_FK = "customer_fk";
  String INVOICE_INVOICEDATE = "invoicedate";
  String INVOICE_BILLINGADDRESS = "billingaddress";
  String INVOICE_BILLINGCITY = "billingcity";
  String INVOICE_BILLINGSTATE = "billingstate";
  String INVOICE_BILLINGCOUNTRY = "billingcountry";
  String INVOICE_BILLINGPOSTALCODE = "billingpostalcode";
  String INVOICE_TOTAL = "total";
  String INVOICE_TOTAL_SUB = "total_sub";

  String T_INVOICELINE = "invoiceline@chinook";
  String INVOICELINE_INVOICELINEID = "invoicelineid";
  String INVOICELINE_INVOICEID = "invoiceid";
  String INVOICELINE_INVOICE_FK = "invoice_fk";
  String INVOICELINE_TRACKID = "trackid";
  String INVOICELINE_TRACK_FK = "track_fk";
  String INVOICELINE_UNITPRICE = "unitprice";
  String INVOICELINE_QUANTITY = "quantity";
  String INVOICELINE_TOTAL = "total";

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
  String PLAYLIST_PLAYLISTID = "playlistid";
  String PLAYLIST_NAME = "name";

  String T_PLAYLISTTRACK = "playlisttrack@chinook";
  String PLAYLISTTRACK_ID = "playlisttrackid";
  String PLAYLISTTRACK_PLAYLISTID = "playlistid";
  String PLAYLISTTRACK_PLAYLIST_FK = "playlist_fk";
  String PLAYLISTTRACK_TRACKID = "trackid";
  String PLAYLISTTRACK_TRACK_FK = "track_fk";
  String PLAYLISTTRACK_ALBUM_DENORM = "album_denorm";
  String PLAYLISTTRACK_ARTIST_DENORM = "artist_denorm";

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
