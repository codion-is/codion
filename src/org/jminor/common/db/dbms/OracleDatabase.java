/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.i18n.Messages;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A Database implementation based on the Oracle database.
 */
public class OracleDatabase extends AbstractDatabase {

  private static final Map<Integer, String> ERROR_CODE_MAP = new HashMap<Integer, String>();

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
  public String getURL(final Properties connectionProperties) {
    return "jdbc:oracle:thin:@" + getHost() + ":" + getPort() + ":" + getSid();
  }

  /**
   * @return false
   */
  @Override
  public boolean supportsIsValid() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String getCheckConnectionQuery() {
    return "select 1 from dual";
  }

  @Override
  public String getErrorMessage(final SQLException exception) {
    if (exception.getErrorCode() == 1400 || exception.getErrorCode() == 1407) {
      final String errorMsg = exception.getMessage();
      final String columnName = errorMsg.substring(errorMsg.lastIndexOf('.') + 2, errorMsg.lastIndexOf(')') - 1);

      return Messages.get(Messages.VALUE_MISSING) + ": " + columnName;
    }

    if (ERROR_CODE_MAP.containsKey(exception.getErrorCode())) {
      return ERROR_CODE_MAP.get(exception.getErrorCode());
    }

    return exception.getMessage();
  }

  @Override
  protected void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded) {
    require(DATABASE_HOST, host);
    require(DATABASE_PORT, port);
    require(DATABASE_SID, sid);
  }
}
