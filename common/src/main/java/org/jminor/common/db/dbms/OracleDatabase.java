/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.Util;
import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.i18n.Messages;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * A Database implementation based on the Oracle database.
 */
public final class OracleDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "oracle.jdbc.OracleDriver";
  static final String URL_PREFIX = "jdbc:oracle:thin:@";
  static final String CHECK_QUERY = "select 1 from dual";

  private static final Map<Integer, String> ERROR_CODE_MAP = new HashMap<>();

  private static final int UNIQUE_KEY_ERROR = 1;
  private static final int CHILD_RECORD_ERROR = 2292;
  private static final int NULL_VALUE_ERROR = 1400;
  private static final int INTEGRITY_CONSTRAINT_ERROR = 2291;
  private static final int NULL_VALUE_ERROR_2 = 1407;
  private static final int CHECK_CONSTRAINT_ERROR = 2290;
  private static final int MISSING_PRIVS_ERROR = 1031;
  private static final int LOGIN_CREDS_ERROR = 1017;
  private static final int TABLE_NOT_FOUND_ERROR = 942;
  private static final int UNABLE_TO_CONNECT_ERROR = 1045;
  private static final int VALUE_TOO_LARGE_ERROR = 1401;
  private static final int VIEW_HAS_ERRORS_ERROR = 4063;

  static {
    ERROR_CODE_MAP.put(UNIQUE_KEY_ERROR, Messages.get(Messages.UNIQUE_KEY_ERROR));
    ERROR_CODE_MAP.put(CHILD_RECORD_ERROR, Messages.get(Messages.CHILD_RECORD_ERROR));
    ERROR_CODE_MAP.put(NULL_VALUE_ERROR, Messages.get(Messages.NULL_VALUE_ERROR));
    ERROR_CODE_MAP.put(INTEGRITY_CONSTRAINT_ERROR, Messages.get(Messages.INTEGRITY_CONSTRAINT_ERROR));
    ERROR_CODE_MAP.put(NULL_VALUE_ERROR_2, Messages.get(Messages.NULL_VALUE_ERROR));
    ERROR_CODE_MAP.put(CHECK_CONSTRAINT_ERROR, Messages.get(Messages.CHECK_CONSTRAINT_ERROR));
    ERROR_CODE_MAP.put(MISSING_PRIVS_ERROR, Messages.get(Messages.MISSING_PRIVILEGES_ERROR));
    ERROR_CODE_MAP.put(LOGIN_CREDS_ERROR, Messages.get(Messages.LOGIN_CREDENTIALS_ERROR));
    ERROR_CODE_MAP.put(TABLE_NOT_FOUND_ERROR, Messages.get(Messages.TABLE_NOT_FOUND_ERROR));
    ERROR_CODE_MAP.put(UNABLE_TO_CONNECT_ERROR, Messages.get(Messages.USER_UNABLE_TO_CONNECT_ERROR));
    ERROR_CODE_MAP.put(VALUE_TOO_LARGE_ERROR, Messages.get(Messages.VALUE_TOO_LARGE_FOR_COLUMN_ERROR));
    ERROR_CODE_MAP.put(VIEW_HAS_ERRORS_ERROR, Messages.get(Messages.VIEW_HAS_ERRORS_ERROR));
  }

  /**
   * Instantiates a new OracleDatabase.
   */
  public OracleDatabase() {
    super(Type.ORACLE, DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new OracleDatabase.
   * @param host the host name
   * @param port the port number
   * @param sid the service identifier
   */
  public OracleDatabase(final String host, final String port, final String sid) {
    super(Type.ORACLE, DRIVER_CLASS_NAME, host, port, sid);
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementValueSQL(final String idSource) {
    Objects.requireNonNull(idSource, "idSource");
    return "select " + idSource + ".currval from dual";
  }

  /** {@inheritDoc} */
  @Override
  public String getSequenceSQL(final String sequenceName) {
    Objects.requireNonNull(sequenceName, "sequenceName");
    return "select " + sequenceName + ".nextval from dual";
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    Util.require("host", getHost());
    Util.require("port", getPort());
    Util.require("sid", getSid());
    return URL_PREFIX + getHost() + ":" + getPort() + ":" + getSid();
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
    return CHECK_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getErrorMessage(final SQLException exception) {
    if (exception.getErrorCode() == NULL_VALUE_ERROR || exception.getErrorCode() == NULL_VALUE_ERROR_2) {
      String exceptionMessage = exception.getMessage();
      final int newlineIndex = exception.getMessage().indexOf('\n');
      if (newlineIndex != -1) {
        exceptionMessage = exceptionMessage.substring(0, newlineIndex);
      }
      final String errorMsg = exceptionMessage;
      final String columnName = errorMsg.substring(errorMsg.lastIndexOf('.') + 2, errorMsg.lastIndexOf(')') - 1);

      return Messages.get(Messages.VALUE_MISSING) + ": " + columnName;
    }

    if (ERROR_CODE_MAP.containsKey(exception.getErrorCode())) {
      return ERROR_CODE_MAP.get(exception.getErrorCode());
    }

    return exception.getMessage();
  }

  /**
   * @param exception the exception
   * @return true if this exception represents a login credentials failure
   */
  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getErrorCode() == LOGIN_CREDS_ERROR;
  }
}
