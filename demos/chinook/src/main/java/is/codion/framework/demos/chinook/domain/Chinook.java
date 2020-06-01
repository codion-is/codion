/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain;

import is.codion.framework.domain.entity.EntityIdentity;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.BlobAttribute;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.EntityAttribute;
import is.codion.plugin.jasperreports.model.JasperReportWrapper;

import java.awt.Image;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static is.codion.framework.domain.property.Identities.entityIdentity;
import static is.codion.plugin.jasperreports.model.JasperReports.classPathReport;

public interface Chinook {

  EntityIdentity T_ARTIST = entityIdentity("artist@chinook");
  Attribute<Long> ARTIST_ARTISTID = T_ARTIST.longAttribute("artistid");
  Attribute<String> ARTIST_NAME = T_ARTIST.stringAttribute("name");
  Attribute<Integer> ARTIST_NR_OF_ALBUMS = T_ARTIST.integerAttribute("nr_of_albums");
  Attribute<Integer> ARTIST_NR_OF_TRACKS = T_ARTIST.integerAttribute("nr_of_tracks");

  EntityIdentity T_ALBUM = entityIdentity("album@chinook");
  Attribute<Long> ALBUM_ALBUMID = T_ALBUM.longAttribute("albumid");
  Attribute<String> ALBUM_TITLE = T_ALBUM.stringAttribute("title");
  Attribute<Long> ALBUM_ARTISTID = T_ALBUM.longAttribute("artistid");
  EntityAttribute ALBUM_ARTIST_FK = T_ALBUM.entityAttribute("artist_fk");
  BlobAttribute ALBUM_COVER = T_ALBUM.blobAttribute("cover");
  Attribute<Image> ALBUM_COVER_IMAGE = T_ALBUM.attribute("coverimage", Image.class);
  Attribute<Integer> ALBUM_NUMBER_OF_TRACKS = T_ALBUM.integerAttribute("nr_of_tracks");

  EntityIdentity T_EMPLOYEE = entityIdentity("employee@chinook");
  Attribute<Long> EMPLOYEE_EMPLOYEEID = T_EMPLOYEE.longAttribute("employeeid");
  Attribute<String> EMPLOYEE_LASTNAME = T_EMPLOYEE.stringAttribute("lastname");
  Attribute<String> EMPLOYEE_FIRSTNAME = T_EMPLOYEE.stringAttribute("firstname");
  Attribute<String> EMPLOYEE_TITLE = T_EMPLOYEE.stringAttribute("title");
  Attribute<Long> EMPLOYEE_REPORTSTO = T_EMPLOYEE.longAttribute("reportsto");
  EntityAttribute EMPLOYEE_REPORTSTO_FK = T_EMPLOYEE.entityAttribute("reportsto_fk");
  Attribute<LocalDate> EMPLOYEE_BIRTHDATE = T_EMPLOYEE.localDateAttribute("birthdate");
  Attribute<LocalDate> EMPLOYEE_HIREDATE = T_EMPLOYEE.localDateAttribute("hiredate");
  Attribute<String> EMPLOYEE_ADDRESS = T_EMPLOYEE.stringAttribute("address");
  Attribute<String> EMPLOYEE_CITY = T_EMPLOYEE.stringAttribute("city");
  Attribute<String> EMPLOYEE_STATE = T_EMPLOYEE.stringAttribute("state");
  Attribute<String> EMPLOYEE_COUNTRY = T_EMPLOYEE.stringAttribute("country");
  Attribute<String> EMPLOYEE_POSTALCODE = T_EMPLOYEE.stringAttribute("postalcode");
  Attribute<String> EMPLOYEE_PHONE = T_EMPLOYEE.stringAttribute("phone");
  Attribute<String> EMPLOYEE_FAX = T_EMPLOYEE.stringAttribute("fax");
  Attribute<String> EMPLOYEE_EMAIL = T_EMPLOYEE.stringAttribute("email");

  EntityIdentity T_CUSTOMER = entityIdentity("customer@chinook");
  Attribute<Long> CUSTOMER_CUSTOMERID = T_CUSTOMER.longAttribute("customerid");
  Attribute<String> CUSTOMER_FIRSTNAME = T_CUSTOMER.stringAttribute("firstname");
  Attribute<String> CUSTOMER_LASTNAME = T_CUSTOMER.stringAttribute("lastname");
  Attribute<String> CUSTOMER_COMPANY = T_CUSTOMER.stringAttribute("company");
  Attribute<String> CUSTOMER_ADDRESS = T_CUSTOMER.stringAttribute("address");
  Attribute<String> CUSTOMER_CITY = T_CUSTOMER.stringAttribute("city");
  Attribute<String> CUSTOMER_STATE = T_CUSTOMER.stringAttribute("state");
  Attribute<String> CUSTOMER_COUNTRY = T_CUSTOMER.stringAttribute("country");
  Attribute<String> CUSTOMER_POSTALCODE = T_CUSTOMER.stringAttribute("postalcode");
  Attribute<String> CUSTOMER_PHONE = T_CUSTOMER.stringAttribute("phone");
  Attribute<String> CUSTOMER_FAX = T_CUSTOMER.stringAttribute("fax");
  Attribute<String> CUSTOMER_EMAIL = T_CUSTOMER.stringAttribute("email");
  Attribute<Long> CUSTOMER_SUPPORTREPID = T_CUSTOMER.longAttribute("supportrepid");
  EntityAttribute CUSTOMER_SUPPORTREP_FK = T_CUSTOMER.entityAttribute("supportrep_fk");

  JasperReportWrapper CUSTOMER_REPORT = classPathReport(Chinook.class, "customer_report.jasper");

  EntityIdentity T_GENRE = entityIdentity("genre@chinook");
  Attribute<Long> GENRE_GENREID = T_GENRE.longAttribute("genreid");
  Attribute<String> GENRE_NAME = T_GENRE.stringAttribute("name");

  EntityIdentity T_MEDIATYPE = entityIdentity("mediatype@chinook");
  Attribute<Long> MEDIATYPE_MEDIATYPEID = T_MEDIATYPE.longAttribute("mediatypeid");
  Attribute<String> MEDIATYPE_NAME = T_MEDIATYPE.stringAttribute("name");

  EntityIdentity T_TRACK = entityIdentity("track@chinook");
  Attribute<Long> TRACK_TRACKID = T_TRACK.longAttribute("trackid");
  Attribute<String> TRACK_NAME = T_TRACK.stringAttribute("name");
  EntityAttribute TRACK_ARTIST_DENORM = T_TRACK.entityAttribute("artist_denorm");
  Attribute<Long> TRACK_ALBUMID = T_TRACK.longAttribute("albumid");
  EntityAttribute TRACK_ALBUM_FK = T_TRACK.entityAttribute("album_fk");
  Attribute<Long> TRACK_MEDIATYPEID = T_TRACK.longAttribute("mediatypeid");
  EntityAttribute TRACK_MEDIATYPE_FK = T_TRACK.entityAttribute("mediatype_fk");
  Attribute<Long> TRACK_GENREID = T_TRACK.longAttribute("genreid");
  EntityAttribute TRACK_GENRE_FK = T_TRACK.entityAttribute("genre_fk");
  Attribute<String> TRACK_COMPOSER = T_TRACK.stringAttribute("composer");
  Attribute<Integer> TRACK_MILLISECONDS = T_TRACK.integerAttribute("milliseconds");
  Attribute<String> TRACK_MINUTES_SECONDS_DERIVED = T_TRACK.stringAttribute("minutes_seconds_transient");
  Attribute<Integer> TRACK_BYTES = T_TRACK.integerAttribute("bytes");
  Attribute<BigDecimal> TRACK_UNITPRICE = T_TRACK.bigDecimalAttribute("unitprice");

  DerivedProperty.Provider<String> TRACK_MIN_SEC_PROVIDER =
          linkedValues -> {
            final Integer milliseconds = (Integer) linkedValues.get(TRACK_MILLISECONDS);
            if (milliseconds == null || milliseconds <= 0) {
              return "";
            }

            return getMinutes(milliseconds) + " min " + getSeconds(milliseconds) + " sec";
          };

  EntityIdentity T_INVOICE = entityIdentity("invoice@chinook");
  Attribute<Long> INVOICE_INVOICEID = T_INVOICE.longAttribute("invoiceid");
  Attribute<Long> INVOICE_CUSTOMERID = T_INVOICE.longAttribute("customerid");
  EntityAttribute INVOICE_CUSTOMER_FK = T_INVOICE.entityAttribute("customer_fk");
  Attribute<LocalDateTime> INVOICE_INVOICEDATE = T_INVOICE.localDateTimeAttribute("invoicedate");
  Attribute<String> INVOICE_BILLINGADDRESS = T_INVOICE.stringAttribute("billingaddress");
  Attribute<String> INVOICE_BILLINGCITY = T_INVOICE.stringAttribute("billingcity");
  Attribute<String> INVOICE_BILLINGSTATE = T_INVOICE.stringAttribute("billingstate");
  Attribute<String> INVOICE_BILLINGCOUNTRY = T_INVOICE.stringAttribute("billingcountry");
  Attribute<String> INVOICE_BILLINGPOSTALCODE = T_INVOICE.stringAttribute("billingpostalcode");
  Attribute<BigDecimal> INVOICE_TOTAL = T_INVOICE.bigDecimalAttribute("total");
  Attribute<BigDecimal> INVOICE_TOTAL_SUB = T_INVOICE.bigDecimalAttribute("total_sub");

  EntityIdentity T_INVOICELINE = entityIdentity("invoiceline@chinook");
  Attribute<Long> INVOICELINE_INVOICELINEID = T_INVOICELINE.longAttribute("invoicelineid");
  Attribute<Long> INVOICELINE_INVOICEID = T_INVOICELINE.longAttribute("invoiceid");
  EntityAttribute INVOICELINE_INVOICE_FK = T_INVOICELINE.entityAttribute("invoice_fk");
  Attribute<Long> INVOICELINE_TRACKID = T_INVOICELINE.longAttribute("trackid");
  EntityAttribute INVOICELINE_TRACK_FK = T_INVOICELINE.entityAttribute("track_fk");
  Attribute<BigDecimal> INVOICELINE_UNITPRICE = T_INVOICELINE.bigDecimalAttribute("unitprice");
  Attribute<Integer> INVOICELINE_QUANTITY = T_INVOICELINE.integerAttribute("quantity");
  Attribute<BigDecimal> INVOICELINE_TOTAL = T_INVOICELINE.bigDecimalAttribute("total");

  DerivedProperty.Provider<BigDecimal> INVOICELINE_TOTAL_PROVIDER =
          linkedValues -> {
            final Integer quantity = (Integer) linkedValues.get(INVOICELINE_QUANTITY);
            final BigDecimal unitPrice = (BigDecimal) linkedValues.get(INVOICELINE_UNITPRICE);
            if (unitPrice == null || quantity == null) {
              return null;
            }

            return unitPrice.multiply(BigDecimal.valueOf(quantity));
          };

  EntityIdentity T_PLAYLIST = entityIdentity("playlist@chinook");
  Attribute<Long> PLAYLIST_PLAYLISTID = T_PLAYLIST.longAttribute("playlistid");
  Attribute<String> PLAYLIST_NAME = T_PLAYLIST.stringAttribute("name");

  EntityIdentity T_PLAYLISTTRACK = entityIdentity("playlisttrack@chinook");
  Attribute<Long> PLAYLISTTRACK_ID = T_PLAYLISTTRACK.longAttribute("playlisttrackid");
  Attribute<Long> PLAYLISTTRACK_PLAYLISTID = T_PLAYLISTTRACK.longAttribute("playlistid");
  EntityAttribute PLAYLISTTRACK_PLAYLIST_FK = T_PLAYLISTTRACK.entityAttribute("playlist_fk");
  Attribute<Long> PLAYLISTTRACK_TRACKID = T_PLAYLISTTRACK.longAttribute("trackid");
  EntityAttribute PLAYLISTTRACK_TRACK_FK = T_PLAYLISTTRACK.entityAttribute("track_fk");
  EntityAttribute PLAYLISTTRACK_ALBUM_DENORM = T_PLAYLISTTRACK.entityAttribute("album_denorm");
  EntityAttribute PLAYLISTTRACK_ARTIST_DENORM = T_PLAYLISTTRACK.entityAttribute("artist_denorm");

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
