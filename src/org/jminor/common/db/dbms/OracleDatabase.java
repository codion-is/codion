/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.i18n.Messages;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OracleDatabase extends AbstractDatabase {

  private static final Map<Integer, String> ERROR_CODE_MAP = new HashMap<Integer, String>();

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("dd-MM-yyyy");
    }
  };
  private static final ThreadLocal timestampFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("dd-MM-yyyy HH:mm");
    }
  };

  static {
    ERROR_CODE_MAP.put(1, Messages.get(Messages.UNIQUE_KEY_ERROR));
    ERROR_CODE_MAP.put(2292, Messages.get(Messages.CHILD_RECORD_ERROR));
    ERROR_CODE_MAP.put(1400, Messages.get(Messages.NULL_VALUE_ERROR));
    ERROR_CODE_MAP.put(2291, Messages.get(Messages.INTEGRITY_CONSTRAINT_ERROR));
    ERROR_CODE_MAP.put(1407, Messages.get(Messages.NULL_VALUE_ERROR));
    ERROR_CODE_MAP.put(2290, Messages.get(Messages.CHECK_CONSTRAINT_ERROR));
    ERROR_CODE_MAP.put(1031, Messages.get(Messages.MISSING_PRIVILEGES_ERROR));
    ERROR_CODE_MAP.put(1017, Messages.get(Messages.LOGIN_CREDENTIALS_ERROR));
    ERROR_CODE_MAP.put(942, Messages.get(Messages.TABLE_NOT_FOUND_ERROR));
    ERROR_CODE_MAP.put(1045, Messages.get(Messages.USER_UNABLE_TO_CONNECT_ERROR));
    ERROR_CODE_MAP.put(1401, Messages.get(Messages.VALUE_TOO_LARGE_FOR_COLUMN_ERROR));
    ERROR_CODE_MAP.put(4063, Messages.get(Messages.VIEW_HAS_ERRORS_ERROR));
  }

  public OracleDatabase() {
    super(ORACLE);
  }

  public OracleDatabase(final String host, final String port, final String sid) {
    super(ORACLE, host, port, sid);
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("oracle.jdbc.OracleDriver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "select " + idSource + ".currval from dual";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    return "select " + sequenceName + ".nextval from dual";
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return isTimestamp ?
            "to_date('" + ((DateFormat) timestampFormat.get()).format(value) + "', 'DD-MM-YYYY HH24:MI')" :
            "to_date('" + ((DateFormat) dateFormat.get()).format(value) + "', 'DD-MM-YYYY')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = getHost();
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());
    final String port = getPort();
    if (port == null || port.length() == 0)
      throw new RuntimeException(DATABASE_PORT + " is required for database type " + getDatabaseType());
    final String sid = getSid();
    if (sid == null || sid.length() == 0)
      throw new RuntimeException(DATABASE_SID + " is required for database type " + getDatabaseType());

    return "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
  }

  /** {@inheritDoc} */
  public String getAuthenticationInfo(final Properties connectionProperties) {
    return null;
  }

  /** {@inheritDoc} */
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return false;
  }

  /** {@inheritDoc} */
  public String getErrorMessage(final SQLException exception) {
    if (exception.getErrorCode() == 1400 || exception.getErrorCode() == 1407) {
      final String errorMsg = exception.getMessage();
      final String columnName = errorMsg.substring(errorMsg.lastIndexOf('.')+2, errorMsg.lastIndexOf(')')-1);

      return Messages.get(Messages.VALUE_MISSING) + ": " + columnName;
    }

    if (ERROR_CODE_MAP.containsKey(exception.getErrorCode()))
      return ERROR_CODE_MAP.get(exception.getErrorCode());

    return exception.getMessage();
  }
}
