/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.h2database;

import is.codion.common.db.database.AbstractDatabase;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the H2 database.
 */
final class H2Database extends AbstractDatabase {

  /**
   * The error code representing incorrect login credentials
   */
  private static final int AUTHENTICATION_ERROR = 28000;
  private static final int REFERENTIAL_INTEGRITY_ERROR_CHILD_EXISTS = 23503;
  private static final int REFERENTIAL_INTEGRITY_ERROR_PARENT_MISSING = 23506;
  private static final int UNIQUE_CONSTRAINT_ERROR = 23505;
  private static final Set<String> INITIALIZED_DATABASES = new HashSet<>();

  private static final String JDBC_URL_PREFIX = "jdbc:h2:";
  private static final String JDBC_URL_PREFIX_MEM = "jdbc:h2:mem:";
  private static final String JDBC_URL_PREFIX_FILE = "jdbc:h2:file:";
  private static final String JDBC_URL_PREFIX_TCP = "jdbc:h2:tcp://";
  private static final String JDBC_URL_PREFIX_SSL = "jdbc:h2:ssl:";
  private static final String JDBC_URL_PREFIX_ZIP = "jdbc:h2:zip:";

  private static final String FILE_SUFFIX_PAGESTORE = ".h2.db";
  private static final String FILE_SUFFIX_MVSTORE = ".mv.db";

  static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String SYSADMIN_USERNAME = "sa";

  private final boolean nowait;

  H2Database(final String jdbcUrl) {
    this(jdbcUrl, emptyList());
  }

  H2Database(final String jdbcUrl, final List<String> scriptPaths) {
    this(jdbcUrl, scriptPaths, true);
  }

  H2Database(final String jdbcUrl, final List<String> scriptPaths, final boolean nowait) {
    super(jdbcUrl);
    this.nowait = nowait;
    synchronized (INITIALIZED_DATABASES) {
      if (!nullOrEmpty(scriptPaths) && !INITIALIZED_DATABASES.contains(jdbcUrl.toLowerCase())) {
        initializeEmbeddedDatabase(scriptPaths);
      }
    }
  }

  @Override
  public String getName() {
    final String name= removeUrlPrefixOptionsAndParameters(getUrl(), JDBC_URL_PREFIX_TCP, JDBC_URL_PREFIX_FILE,
            JDBC_URL_PREFIX_MEM, JDBC_URL_PREFIX_SSL, JDBC_URL_PREFIX_ZIP, JDBC_URL_PREFIX);

    return name.isEmpty() ? "private" : name;
  }

  @Override
  public String getSelectForUpdateClause() {
    if (nowait) {
      return FOR_UPDATE_NOWAIT;
    }

    return FOR_UPDATE;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String getSequenceQuery(final String sequenceName) {
    return SEQUENCE_VALUE_QUERY + requireNonNull(sequenceName, "sequenceName");
  }

  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getErrorCode() == AUTHENTICATION_ERROR;
  }

  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_INTEGRITY_ERROR_CHILD_EXISTS ||
            exception.getErrorCode() == REFERENTIAL_INTEGRITY_ERROR_PARENT_MISSING;
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR;
  }

  private void initializeEmbeddedDatabase(final List<String> scriptPaths) {
    if ((isEmbeddedInMemory() || !databaseFileExists())) {
      final Properties properties = new Properties();
      properties.put(USER_PROPERTY, SYSADMIN_USERNAME);
      for (final String scriptPath : scriptPaths) {
        final String initUrl = getUrl() + ";DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM '" + scriptPath.replace("\\", "/") + "'";
        try {
          DriverManager.getConnection(initUrl, properties).close();
        }
        catch (final SQLException e) {
          throw new RuntimeException(e);
        }
      }
    }
    INITIALIZED_DATABASES.add(getUrl().toLowerCase());
  }

  private String getDatabasePath() {
    return removeUrlPrefixOptionsAndParameters(getUrl(), JDBC_URL_PREFIX_FILE, JDBC_URL_PREFIX);
  }

  private boolean isEmbeddedInMemory() {
    return getUrl().startsWith(JDBC_URL_PREFIX_MEM);
  }

  private boolean databaseFileExists() {
    return Files.exists(Paths.get(getDatabasePath() + FILE_SUFFIX_PAGESTORE)) ||
            Files.exists(Paths.get(getDatabasePath() + FILE_SUFFIX_MVSTORE));
  }
}
