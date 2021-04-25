/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.oracle;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the Oracle database.
 */
final class OracleDatabase extends AbstractDatabase {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(OracleDatabase.class.getName());

  private static final String JDBC_URL_PREFIX = "jdbc:oracle:thin:@";

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

  static final String CHECK_QUERY = "select 1 from dual";

  static {
    ERROR_CODE_MAP.put(UNIQUE_KEY_ERROR, MESSAGES.getString("unique_key_error"));
    ERROR_CODE_MAP.put(CHILD_RECORD_ERROR, MESSAGES.getString("child_record_error"));
    ERROR_CODE_MAP.put(NULL_VALUE_ERROR, MESSAGES.getString("null_value_error"));
    ERROR_CODE_MAP.put(INTEGRITY_CONSTRAINT_ERROR, MESSAGES.getString("integrity_constraint_error"));
    ERROR_CODE_MAP.put(NULL_VALUE_ERROR_2, MESSAGES.getString("null_value_error"));
    ERROR_CODE_MAP.put(CHECK_CONSTRAINT_ERROR, MESSAGES.getString("check_constraint_error"));
    ERROR_CODE_MAP.put(MISSING_PRIVS_ERROR, MESSAGES.getString("missing_privileges_error"));
    ERROR_CODE_MAP.put(LOGIN_CREDS_ERROR, MESSAGES.getString("login_credentials_error"));
    ERROR_CODE_MAP.put(TABLE_NOT_FOUND_ERROR, MESSAGES.getString("table_not_found_error"));
    ERROR_CODE_MAP.put(UNABLE_TO_CONNECT_ERROR, MESSAGES.getString("user_cannot_connect"));
    ERROR_CODE_MAP.put(VALUE_TOO_LARGE_ERROR, MESSAGES.getString("value_too_large_for_column_error"));
    ERROR_CODE_MAP.put(VIEW_HAS_ERRORS_ERROR, MESSAGES.getString("view_has_errors_error"));
  }

  OracleDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = removeUrlPrefixOptionsAndParameters(getUrl(), JDBC_URL_PREFIX);
    if (name.contains("/")) {//pluggable database
      name = name.substring(name.lastIndexOf('/') + 1);
    }

    return name.substring(name.lastIndexOf(':') + 1);
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return "select " + requireNonNull(idSource, "idSource") + ".currval from dual";
  }

  @Override
  public String getSequenceQuery(final String sequenceName) {
    return "select " + requireNonNull(sequenceName, "sequenceName") + ".nextval from dual";
  }

  /**
   * @return false
   */
  @Override
  public boolean supportsIsValid() {
    return false;
  }

  @Override
  public String getCheckConnectionQuery() {
    return CHECK_QUERY;
  }

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

      return MESSAGES.getString("value_missing") + ": " + columnName;
    }

    if (ERROR_CODE_MAP.containsKey(exception.getErrorCode())) {
      return ERROR_CODE_MAP.get(exception.getErrorCode());
    }

    return exception.getMessage();
  }

  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getErrorCode() == LOGIN_CREDS_ERROR;
  }

  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == CHILD_RECORD_ERROR || exception.getErrorCode() == INTEGRITY_CONSTRAINT_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getErrorCode() == UNIQUE_KEY_ERROR;
  }
}
