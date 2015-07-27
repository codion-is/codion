/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Item;

import java.awt.Color;
import java.sql.Types;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class TestDomain {

  private TestDomain() {}
  public static void init() {}

  public static final String T_MASTER = "test.master_entity";
  public static final String MASTER_ID = "id";
  public static final String MASTER_NAME = "name";
  public static final String MASTER_CODE = "code";

  public static final String T_DETAIL = "test.detail_entity";
  public static final String DETAIL_ID = "id";
  public static final String DETAIL_INT = "int";
  public static final String DETAIL_DOUBLE = "double";
  public static final String DETAIL_STRING = "string";
  public static final String DETAIL_DATE = "date";
  public static final String DETAIL_TIMESTAMP = "timestamp";
  public static final String DETAIL_BOOLEAN = "boolean";
  public static final String DETAIL_BOOLEAN_NULLABLE = "boolean_nullable";
  public static final String DETAIL_ENTITY_ID = "entity_id";
  public static final String DETAIL_ENTITY_FK = "entity_ref";
  public static final String DETAIL_MASTER_NAME = "master_name";
  public static final String DETAIL_MASTER_CODE = "master_code";
  public static final String DETAIL_INT_VALUE_LIST = "int_value_list";
  public static final String DETAIL_INT_DERIVED = "int_derived";

  private static final List<Item> ITEMS = Arrays.asList(new Item(0, "0"), new Item(1, "1"),
          new Item(2, "2"), new Item(3, "3"));

  public static final String DETAIL_SELECT_TABLE_NAME = "test.entity_test_select";

  static {
    Entities.define(T_MASTER,
            Properties.primaryKeyProperty(MASTER_ID, Types.BIGINT),
            Properties.columnProperty(MASTER_NAME, Types.VARCHAR),
            Properties.columnProperty(MASTER_CODE, Types.INTEGER))
            .setComparator(new Comparator<Entity>() {
              @Override
              public int compare(final Entity o1, final Entity o2) {
                final Integer code1 = o1.getIntValue(MASTER_CODE);
                final Integer code2 = o2.getIntValue(MASTER_CODE);

                return code1.compareTo(code2);
              }
            })
            .setStringProvider(new Entities.StringProvider(MASTER_NAME));

    Entities.define(T_DETAIL,
            Properties.primaryKeyProperty(DETAIL_ID, Types.BIGINT),
            Properties.columnProperty(DETAIL_INT, Types.INTEGER, DETAIL_INT),
            Properties.columnProperty(DETAIL_DOUBLE, Types.DOUBLE, DETAIL_DOUBLE),
            Properties.columnProperty(DETAIL_STRING, Types.VARCHAR, "Detail string"),
            Properties.columnProperty(DETAIL_DATE, Types.DATE, DETAIL_DATE),
            Properties.columnProperty(DETAIL_TIMESTAMP, Types.TIMESTAMP, DETAIL_TIMESTAMP),
            Properties.columnProperty(DETAIL_BOOLEAN, Types.BOOLEAN, DETAIL_BOOLEAN)
                    .setNullable(false)
                    .setDefaultValue(true)
                    .setDescription("A boolean property"),
            Properties.columnProperty(DETAIL_BOOLEAN_NULLABLE, Types.BOOLEAN, DETAIL_BOOLEAN_NULLABLE)
                    .setDefaultValue(true),
            Properties.foreignKeyProperty(DETAIL_ENTITY_FK, DETAIL_ENTITY_FK, T_MASTER,
                    Properties.columnProperty(DETAIL_ENTITY_ID, Types.BIGINT)),
            Properties.denormalizedViewProperty(DETAIL_MASTER_NAME, DETAIL_ENTITY_FK,
                    Entities.getProperty(T_MASTER, MASTER_NAME), DETAIL_MASTER_NAME),
            Properties.denormalizedViewProperty(DETAIL_MASTER_CODE, DETAIL_ENTITY_FK,
                    Entities.getProperty(T_MASTER, MASTER_CODE), DETAIL_MASTER_CODE),
            Properties.valueListProperty(DETAIL_INT_VALUE_LIST, Types.INTEGER, DETAIL_INT_VALUE_LIST, ITEMS),
            Properties.derivedProperty(DETAIL_INT_DERIVED, Types.INTEGER, DETAIL_INT_DERIVED, new Property.DerivedProperty.Provider() {
              @Override
              public Object getValue(final Map<String, Object> linkedValues) {
                final Integer intValue = (Integer) linkedValues.get(DETAIL_INT);
                if (intValue == null) {
                  return null;
                }

                return intValue * 10;
              }
            }, DETAIL_INT))
            .setOrderByClause(DETAIL_STRING)
            .setSelectTableName(DETAIL_SELECT_TABLE_NAME)
            .setSmallDataset(true)
            .setStringProvider(new Entities.StringProvider(DETAIL_STRING));
  }

  public static final String T_DEPARTMENT = "unittest.scott.dept";
  public static final String DEPARTMENT_ID = "deptno";
  public static final String DEPARTMENT_NAME = "dname";
  public static final String DEPARTMENT_LOCATION = "loc";

  public static final String T_EMP = "unittest.scott.emp";
  public static final String EMP_ID = "empno";
  public static final String EMP_NAME = "ename";
  public static final String EMP_JOB = "job";
  public static final String EMP_MGR = "mgr";
  public static final String EMP_HIREDATE = "hiredate";
  public static final String EMP_SALARY = "sal";
  public static final String EMP_COMMISSION = "comm";
  public static final String EMP_DEPARTMENT = "deptno";
  public static final String EMP_DEPARTMENT_FK = "dept_fk";
  public static final String EMP_MGR_FK = "mgr_fk";
  public static final String EMP_DEPARTMENT_LOCATION = "location";

  static {
    Entities.define(T_DEPARTMENT, "scott.dept",
            Properties.primaryKeyProperty(DEPARTMENT_ID, Types.INTEGER, DEPARTMENT_ID)
                    .setUpdatable(true).setNullable(false),
            Properties.columnProperty(DEPARTMENT_NAME, Types.VARCHAR, DEPARTMENT_NAME)
                    .setPreferredColumnWidth(120).setMaxLength(14).setNullable(false),
            Properties.columnProperty(DEPARTMENT_LOCATION, Types.VARCHAR, DEPARTMENT_LOCATION)
                    .setPreferredColumnWidth(150).setMaxLength(13))
            .setSmallDataset(true)
            .setOrderByClause(DEPARTMENT_NAME)
            .setStringProvider(new Entities.StringProvider(DEPARTMENT_NAME))
            .setCaption("Department");

    Entities.define(T_EMP, "scott.emp",
            Properties.primaryKeyProperty(EMP_ID, Types.INTEGER, EMP_ID),
            Properties.columnProperty(EMP_NAME, Types.VARCHAR, EMP_NAME)
                    .setMaxLength(10).setNullable(false),
            Properties.foreignKeyProperty(EMP_DEPARTMENT_FK, EMP_DEPARTMENT_FK, T_DEPARTMENT,
                    Properties.columnProperty(EMP_DEPARTMENT))
                    .setNullable(false),
            Properties.columnProperty(EMP_JOB, Types.VARCHAR, EMP_JOB)
                    .setMaxLength(9),
            Properties.columnProperty(EMP_SALARY, Types.DOUBLE, EMP_SALARY)
                    .setNullable(false).setMin(1000).setMax(10000).setMaximumFractionDigits(2),
            Properties.columnProperty(EMP_COMMISSION, Types.DOUBLE, EMP_COMMISSION)
                    .setMin(100).setMax(2000).setMaximumFractionDigits(2),
            Properties.foreignKeyProperty(EMP_MGR_FK, EMP_MGR_FK, T_EMP,
                    Properties.columnProperty(EMP_MGR)),
            Properties.columnProperty(EMP_HIREDATE, Types.DATE, EMP_HIREDATE)
                    .setNullable(false),
            Properties.denormalizedViewProperty(EMP_DEPARTMENT_LOCATION, EMP_DEPARTMENT_FK,
                    Entities.getProperty(T_DEPARTMENT, DEPARTMENT_LOCATION),
                    DEPARTMENT_LOCATION).setPreferredColumnWidth(100))
            .setKeyGenerator(Entities.incrementKeyGenerator("scott.emp", EMP_ID))
            .setOrderByClause(EMP_DEPARTMENT + ", " + EMP_NAME)
            .setStringProvider(new Entities.StringProvider(EMP_NAME))
            .setCaption("Employee")
            .setBackgroundColorProvider(new Entity.BackgroundColorProvider() {
              /*provide a custom background color for managers*/
              @Override
              public Color getBackgroundColor(final Entity entity, final Property property) {
                if (property.is(EMP_JOB) && "MANAGER".equals(entity.getValue(EMP_JOB))) {
                  return Color.CYAN;
                }

                return null;
              }
            });
  }

  public static final String DOMAIN_ID = TestDomain.class.getName();

  public static final String T_ARTIST = "unittest.chinook.artist";
  public static final String ARTIST_ARTISTID = "artistid";
  public static final String ARTIST_NAME = "name";

  static {
    Entities.define(T_ARTIST, "chinook.artist",
            Properties.primaryKeyProperty(ARTIST_ARTISTID, Types.BIGINT),
            Properties.columnProperty(ARTIST_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_ARTIST));
  }

  public static final String T_ALBUM = "unittest.chinook.album";
  public static final String ALBUM_ALBUMID = "albumid";
  public static final String ALBUM_TITLE = "title";
  public static final String ALBUM_ARTISTID = "artistid";
  public static final String ALBUM_ARTISTID_FK = "artistid_fk";

  static {
    Entities.define(T_ALBUM, "chinook.albutm",
            Properties.primaryKeyProperty(ALBUM_ALBUMID, Types.BIGINT),
            Properties.foreignKeyProperty(ALBUM_ARTISTID_FK, "Artist", T_ARTIST,
                    Properties.columnProperty(ALBUM_ARTISTID, Types.BIGINT))
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            Properties.columnProperty(ALBUM_TITLE, Types.VARCHAR, "Title")
                    .setNullable(false)
                    .setMaxLength(160)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_ALBUM));
  }

  public static final String T_EMPLOYEE = "unittest.chinook.employee";
  public static final String EMPLOYEE_EMPLOYEEID = "employeeid";
  public static final String EMPLOYEE_LASTNAME = "lastname";
  public static final String EMPLOYEE_FIRSTNAME = "firstname";
  public static final String EMPLOYEE_TITLE = "title";
  public static final String EMPLOYEE_REPORTSTO = "reportsto";
  public static final String EMPLOYEE_REPORTSTO_FK = "reportsto_fk";
  public static final String EMPLOYEE_BIRTHDATE = "birthdate";
  public static final String EMPLOYEE_HIREDATE = "hiredate";
  public static final String EMPLOYEE_ADDRESS = "address";
  public static final String EMPLOYEE_CITY = "city";
  public static final String EMPLOYEE_STATE = "state";
  public static final String EMPLOYEE_COUNTRY = "country";
  public static final String EMPLOYEE_POSTALCODE = "postalcode";
  public static final String EMPLOYEE_PHONE = "phone";
  public static final String EMPLOYEE_FAX = "fax";
  public static final String EMPLOYEE_EMAIL = "email";

  static {
    Entities.define(T_EMPLOYEE, "chinook.employee",
            Properties.primaryKeyProperty(EMPLOYEE_EMPLOYEEID, Types.BIGINT),
            Properties.columnProperty(EMPLOYEE_LASTNAME, Types.VARCHAR, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(EMPLOYEE_FIRSTNAME, Types.VARCHAR, "First name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(EMPLOYEE_TITLE, Types.VARCHAR, "Title")
                    .setMaxLength(30),
            Properties.foreignKeyProperty(EMPLOYEE_REPORTSTO_FK, "Reports to", T_EMPLOYEE,
                    Properties.columnProperty(EMPLOYEE_REPORTSTO, Types.BIGINT)),
            Properties.columnProperty(EMPLOYEE_BIRTHDATE, Types.DATE, "Birthdate"),
            Properties.columnProperty(EMPLOYEE_HIREDATE, Types.DATE, "Hiredate"),
            Properties.columnProperty(EMPLOYEE_ADDRESS, Types.VARCHAR, "Address")
                    .setMaxLength(70),
            Properties.columnProperty(EMPLOYEE_CITY, Types.VARCHAR, "City")
                    .setMaxLength(40),
            Properties.columnProperty(EMPLOYEE_STATE, Types.VARCHAR, "State")
                    .setMaxLength(40),
            Properties.columnProperty(EMPLOYEE_COUNTRY, Types.VARCHAR, "Country")
                    .setMaxLength(40),
            Properties.columnProperty(EMPLOYEE_POSTALCODE, Types.VARCHAR, "Postal code")
                    .setMaxLength(10),
            Properties.columnProperty(EMPLOYEE_PHONE, Types.VARCHAR, "Phone")
                    .setMaxLength(24),
            Properties.columnProperty(EMPLOYEE_FAX, Types.VARCHAR, "Fax")
                    .setMaxLength(24),
            Properties.columnProperty(EMPLOYEE_EMAIL, Types.VARCHAR, "Email")
                    .setMaxLength(60))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_EMPLOYEE));
  }

  public static final String T_CUSTOMER = "unittest.chinook.customer";
  public static final String CUSTOMER_CUSTOMERID = "customerid";
  public static final String CUSTOMER_FIRSTNAME = "firstname";
  public static final String CUSTOMER_LASTNAME = "lastname";
  public static final String CUSTOMER_COMPANY = "company";
  public static final String CUSTOMER_ADDRESS = "address";
  public static final String CUSTOMER_CITY = "city";
  public static final String CUSTOMER_STATE = "state";
  public static final String CUSTOMER_COUNTRY = "country";
  public static final String CUSTOMER_POSTALCODE = "postalcode";
  public static final String CUSTOMER_PHONE = "phone";
  public static final String CUSTOMER_FAX = "fax";
  public static final String CUSTOMER_EMAIL = "email";
  public static final String CUSTOMER_SUPPORTREPID = "supportrepid";
  public static final String CUSTOMER_SUPPORTREPID_FK = "supportrepid_fk";

  static {
    Entities.define(T_CUSTOMER, "chinook.customer",
            Properties.primaryKeyProperty(CUSTOMER_CUSTOMERID, Types.BIGINT),
            Properties.columnProperty(CUSTOMER_LASTNAME, Types.VARCHAR, "Last name")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(CUSTOMER_FIRSTNAME, Types.VARCHAR, "First name")
                    .setNullable(false)
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_COMPANY, Types.VARCHAR, "Company")
                    .setMaxLength(80),
            Properties.columnProperty(CUSTOMER_ADDRESS, Types.VARCHAR, "Address")
                    .setMaxLength(70),
            Properties.columnProperty(CUSTOMER_CITY, Types.VARCHAR, "City")
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_STATE, Types.VARCHAR, "State")
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_COUNTRY, Types.VARCHAR, "Country")
                    .setMaxLength(40),
            Properties.columnProperty(CUSTOMER_POSTALCODE, Types.VARCHAR, "Postal code")
                    .setMaxLength(10),
            Properties.columnProperty(CUSTOMER_PHONE, Types.VARCHAR, "Phone")
                    .setMaxLength(24),
            Properties.columnProperty(CUSTOMER_FAX, Types.VARCHAR, "Fax")
                    .setMaxLength(24),
            Properties.columnProperty(CUSTOMER_EMAIL, Types.VARCHAR, "Email")
                    .setNullable(false)
                    .setMaxLength(60),
            Properties.foreignKeyProperty(CUSTOMER_SUPPORTREPID_FK, "Support rep", T_EMPLOYEE,
                    Properties.columnProperty(CUSTOMER_SUPPORTREPID, Types.BIGINT)))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_CUSTOMER))
            .setSearchPropertyIDs(CUSTOMER_FIRSTNAME, CUSTOMER_LASTNAME, CUSTOMER_EMAIL);
  }

  public static final String T_GENRE = "unittest.chinook.genre";
  public static final String GENRE_GENREID = "genreid";
  public static final String GENRE_NAME = "name";

  static {
    Entities.define(T_GENRE, "chinook.genre",
            Properties.primaryKeyProperty(GENRE_GENREID, Types.BIGINT),
            Properties.columnProperty(GENRE_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_GENRE));
  }

  public static final String T_MEDIATYPE = "unittest.chinook.mediatype";
  public static final String MEDIATYPE_MEDIATYPEID = "mediatypeid";
  public static final String MEDIATYPE_NAME = "name";

  static {
    Entities.define(T_MEDIATYPE, "chinook.mediatype",
            Properties.primaryKeyProperty(MEDIATYPE_MEDIATYPEID, Types.BIGINT),
            Properties.columnProperty(MEDIATYPE_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_MEDIATYPE));
  }

  public static final String T_TRACK = "unittest.chinook.track";
  public static final String TRACK_TRACKID = "trackid";
  public static final String TRACK_NAME = "name";
  public static final String TRACK_ARTIST_DENORM = "artist_denorm";
  public static final String TRACK_ALBUMID = "albumid";
  public static final String TRACK_ALBUMID_FK = "albumid_fk";
  public static final String TRACK_MEDIATYPEID = "mediatypeid";
  public static final String TRACK_MEDIATYPEID_FK = "mediatypeid_fk";
  public static final String TRACK_GENREID = "genreid";
  public static final String TRACK_GENREID_FK = "genreid_fk";
  public static final String TRACK_COMPOSER = "composer";
  public static final String TRACK_MILLISECONDS = "milliseconds";
  public static final String TRACK_MINUTES_SECONDS_DERIVED = "minutes_seconds_transient";
  public static final String TRACK_BYTES = "bytes";
  public static final String TRACK_UNITPRICE = "unitprice";

  public static final Property.DerivedProperty.Provider TRACK_MIN_SEC_PROVIDER =
          new Property.DerivedProperty.Provider() {
            @Override
            public Object getValue(final Map<String, Object> linkedValues) {
              final Integer milliseconds = (Integer) linkedValues.get(TRACK_MILLISECONDS);
              if (milliseconds == null || milliseconds <= 0) {
                return "";
              }

              final int seconds = ((milliseconds / 1000) % 60);
              final int minutes = ((milliseconds / 1000) / 60);

              return minutes + " min " + seconds + " sec";
            }
          };

  static {
    Entities.define(T_TRACK, "chinook.track",
            Properties.primaryKeyProperty(TRACK_TRACKID, Types.BIGINT),
            Properties.denormalizedViewProperty(TRACK_ARTIST_DENORM, TRACK_ALBUMID_FK,
                    Entities.getProperty(T_ALBUM, ALBUM_ARTISTID_FK), "Artist")
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_ALBUMID_FK, "Album", T_ALBUM,
                    Properties.columnProperty(TRACK_ALBUMID, Types.BIGINT))
                    .setFetchDepth(2)
                    .setPreferredColumnWidth(160),
            Properties.columnProperty(TRACK_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(200)
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_GENREID_FK, "Genre", T_GENRE,
                    Properties.columnProperty(TRACK_GENREID, Types.BIGINT)),
            Properties.columnProperty(TRACK_COMPOSER, Types.VARCHAR, "Composer")
                    .setMaxLength(220)
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(TRACK_MEDIATYPEID_FK, "Media type", T_MEDIATYPE,
                    Properties.columnProperty(TRACK_MEDIATYPEID, Types.BIGINT))
                    .setNullable(false),
            Properties.columnProperty(TRACK_MILLISECONDS, Types.INTEGER, "Duration (ms)")
                    .setNullable(false),
            Properties.derivedProperty(TRACK_MINUTES_SECONDS_DERIVED, Types.VARCHAR, "Duration (min/sec)",
                    TRACK_MIN_SEC_PROVIDER, TRACK_MILLISECONDS),
            Properties.columnProperty(TRACK_BYTES, Types.INTEGER, "Bytes"),
            Properties.columnProperty(TRACK_UNITPRICE, Types.DOUBLE, "Price")
                    .setNullable(false))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_TRACK));
  }

  public static final String T_INVOICE = "unittest.chinook.invoice";
  public static final String INVOICE_INVOICEID = "invoiceid";
  public static final String INVOICE_CUSTOMERID = "customerid";
  public static final String INVOICE_CUSTOMERID_FK = "customerid_fk";
  public static final String INVOICE_INVOICEDATE = "invoicedate";
  public static final String INVOICE_BILLINGADDRESS = "billingaddress";
  public static final String INVOICE_BILLINGCITY = "billingcity";
  public static final String INVOICE_BILLINGSTATE = "billingstate";
  public static final String INVOICE_BILLINGCOUNTRY = "billingcountry";
  public static final String INVOICE_BILLINGPOSTALCODE = "billingpostalcode";
  public static final String INVOICE_TOTAL = "total";
  public static final String INVOICE_TOTAL_SUB = "total_sub";
  public static final String INVOICE_TOTAL_SUBQUERY = "select sum(unitprice * quantity) from chinook.invoiceline where invoiceid = invoice.invoiceid";

  static {
    Entities.define(T_INVOICE, "chinook.invoice",
            Properties.primaryKeyProperty(INVOICE_INVOICEID, Types.BIGINT, "Invoice no."),
            Properties.foreignKeyProperty(INVOICE_CUSTOMERID_FK, "Customer", T_CUSTOMER,
                    Properties.columnProperty(INVOICE_CUSTOMERID, Types.BIGINT))
                    .setNullable(false),
            Properties.columnProperty(INVOICE_INVOICEDATE, Types.DATE, "Date")
                    .setNullable(false),
            Properties.columnProperty(INVOICE_BILLINGADDRESS, Types.VARCHAR, "Billing address")
                    .setMaxLength(70),
            Properties.columnProperty(INVOICE_BILLINGCITY, Types.VARCHAR, "Billing city")
                    .setMaxLength(40),
            Properties.columnProperty(INVOICE_BILLINGSTATE, Types.VARCHAR, "Billing state")
                    .setMaxLength(40),
            Properties.columnProperty(INVOICE_BILLINGCOUNTRY, Types.VARCHAR, "Billing country")
                    .setMaxLength(40),
            Properties.columnProperty(INVOICE_BILLINGPOSTALCODE, Types.VARCHAR, "Billing postal code")
                    .setMaxLength(10),
            Properties.columnProperty(INVOICE_TOTAL, Types.DOUBLE, "Total")
                    .setMaximumFractionDigits(2)
                    .setHidden(true),
            Properties.subqueryProperty(INVOICE_TOTAL_SUB, Types.DOUBLE, "Calculated total", INVOICE_TOTAL_SUBQUERY)
                    .setMaximumFractionDigits(2))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_INVOICE));
  }

  public static final String T_INVOICELINE = "unittest.chinook.invoiceline";
  public static final String INVOICELINE_INVOICELINEID = "invoicelineid";
  public static final String INVOICELINE_INVOICEID = "invoiceid";
  public static final String INVOICELINE_INVOICEID_FK = "invoiceid_fk";
  public static final String INVOICELINE_TRACKID = "trackid";
  public static final String INVOICELINE_TRACKID_FK = "trackid_fk";
  public static final String INVOICELINE_UNITPRICE = "unitprice";
  public static final String INVOICELINE_QUANTITY = "quantity";
  public static final String INVOICELINE_TOTAL = "total";

  public static final Property.DerivedProperty.Provider INVOICELINE_TOTAL_PROVIDER =
          new Property.DerivedProperty.Provider() {
            @Override
            public Object getValue(final Map<String, Object> linkedValues) {
              final Integer quantity = (Integer) linkedValues.get(INVOICELINE_QUANTITY);
              final Double unitPrice = (Double) linkedValues.get(INVOICELINE_UNITPRICE);
              if (unitPrice == null || quantity == null) {
                return null;
              }

              return quantity * unitPrice;
            }
          };

  static {
    Entities.define(T_INVOICELINE, "chinook.invoiceline",
            Properties.primaryKeyProperty(INVOICELINE_INVOICELINEID, Types.BIGINT),
            Properties.foreignKeyProperty(INVOICELINE_INVOICEID_FK, "Invoice", T_INVOICE,
                    Properties.columnProperty(INVOICELINE_INVOICEID, Types.BIGINT))
                    .setFetchDepth(0)
                    .setNullable(false),
            Properties.foreignKeyProperty(INVOICELINE_TRACKID_FK, "Track", T_TRACK,
                    Properties.columnProperty(INVOICELINE_TRACKID, Types.BIGINT))
                    .setNullable(false)
                    .setPreferredColumnWidth(100),
            Properties.denormalizedProperty(INVOICELINE_UNITPRICE, INVOICELINE_TRACKID_FK,
                    Entities.getProperty(T_TRACK, TRACK_UNITPRICE), "Unit price")
                    .setNullable(false),
            Properties.columnProperty(INVOICELINE_QUANTITY, Types.INTEGER, "Quantity")
                    .setNullable(false),
            Properties.derivedProperty(INVOICELINE_TOTAL, Types.DOUBLE, "Total", INVOICELINE_TOTAL_PROVIDER,
                    INVOICELINE_QUANTITY, INVOICELINE_UNITPRICE))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_INVOICELINE));
  }

  public static final String T_PLAYLIST = "unittest.chinook.playlist";
  public static final String PLAYLIST_PLAYLISTID = "playlistid";
  public static final String PLAYLIST_NAME = "name";

  static {
    Entities.define(T_PLAYLIST, "chinook.playlist",
            Properties.primaryKeyProperty(PLAYLIST_PLAYLISTID, Types.BIGINT),
            Properties.columnProperty(PLAYLIST_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(120)
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID)
            .setKeyGenerator(Entities.automaticKeyGenerator(T_PLAYLIST));
  }

  public static final String T_PLAYLISTTRACK = "unittest.chinook.playlisttrack";
  public static final String PLAYLISTTRACK_PLAYLISTID = "playlistid";
  public static final String PLAYLISTTRACK_PLAYLISTID_FK = "playlistid_fk";
  public static final String PLAYLISTTRACK_TRACKID = "trackid";
  public static final String PLAYLISTTRACK_TRACKID_FK = "trackid_fk";
  public static final String PLAYLISTTRACK_ALBUM_DENORM = "album_denorm";
  public static final String PLAYLISTTRACK_ARTIST_DENORM = "artist_denorm";

  static {
    Entities.define(T_PLAYLISTTRACK, "chinook.playlisttrack",
            Properties.foreignKeyProperty(PLAYLISTTRACK_PLAYLISTID_FK, "Playlist", T_PLAYLIST,
                    Properties.primaryKeyProperty(PLAYLISTTRACK_PLAYLISTID, Types.BIGINT)
                            .setUpdatable(true))
                    .setNullable(false)
                    .setPreferredColumnWidth(120),
            Properties.denormalizedViewProperty(PLAYLISTTRACK_ARTIST_DENORM, PLAYLISTTRACK_ALBUM_DENORM,
                    Entities.getProperty(T_ALBUM, ALBUM_ARTISTID_FK), "Artist")
                    .setPreferredColumnWidth(160),
            Properties.foreignKeyProperty(PLAYLISTTRACK_TRACKID_FK, "Track", T_TRACK,
                    Properties.primaryKeyProperty(PLAYLISTTRACK_TRACKID, Types.BIGINT)
                            .setPrimaryKeyIndex(1)
                            .setUpdatable(true))
                    .setFetchDepth(3)
                    .setNullable(false)
                    .setPreferredColumnWidth(160),
            Properties.denormalizedViewProperty(PLAYLISTTRACK_ALBUM_DENORM, PLAYLISTTRACK_TRACKID_FK,
                    Entities.getProperty(T_TRACK, TRACK_ALBUMID_FK), "Album")
                    .setPreferredColumnWidth(160))
            .setDomainID(DOMAIN_ID);
  }
}
