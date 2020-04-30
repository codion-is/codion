/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.h2database;

import org.jminor.common.db.database.AbstractDatabase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

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
  static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
  static final String URL_PREFIX_MEM = "jdbc:h2:mem:";
  static final String URL_PREFIX_FILE = "jdbc:h2:file:";

  /**
   * Instantiates a new embedded H2Database.
   * @param jdbcUrl the jdbc url
   */
  H2Database(final String jdbcUrl) {
    this(jdbcUrl, emptyList());
  }

  /**
   * Instantiates a new embedded H2Database.
   * @param jdbcUrl the jdbc url
   * @param scriptPaths paths to the scripts to run to initialize the database
   */
  H2Database(final String jdbcUrl, final List<String> scriptPaths) {
    super(jdbcUrl);
    initializeEmbeddedDatabase(scriptPaths);
  }

  @Override
  public String getName() {
    String name = getURL();
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX_TCP)) {
      name = name.substring(JDBC_URL_PREFIX_TCP.length());
    }
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX_FILE)) {
      name = name.substring(JDBC_URL_PREFIX_FILE.length());
    }
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX_MEM)) {
      name = name.substring(JDBC_URL_PREFIX_MEM.length());
    }
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX_SSL)) {
      name = name.substring(JDBC_URL_PREFIX_SSL.length());
    }
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX_ZIP)) {
      name = name.substring(JDBC_URL_PREFIX_ZIP.length());
    }
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX)) {
      name = name.substring(JDBC_URL_PREFIX.length());
    }
    if (name.contains(";")) {
      name = name.substring(0, name.indexOf(';'));
    }

    return name.isEmpty() ? "private" : name;
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

  /**
   * Runs the given script using the RunScript tool, with the default sysadmin username (sa) and default charset
   * @param scriptPath the path to the script
   * @throws SQLException in case of an exception
   */
  public void runScript(final String scriptPath) throws SQLException {
    runScript(scriptPath, SYSADMIN_USERNAME, "", Charset.defaultCharset());
  }

  /**
   * Runs the given script using the RunScript tool
   * @param scriptPath the path to the script
   * @param username the username to run the script under
   * @param password the password
   * @param scriptCharset the script character set
   * @throws SQLException in case of an exception
   */
  public void runScript(final String scriptPath, final String username, final String password, final Charset scriptCharset) throws SQLException {
    try {
      final Class runScriptToolClass = Class.forName(RUN_TOOL_CLASS_NAME);
      final Method execute = runScriptToolClass.getMethod("execute", String.class, String.class, String.class, String.class, Charset.class, boolean.class);
      execute.invoke(runScriptToolClass.getDeclaredConstructor().newInstance(), getURL(), username, password, scriptPath, scriptCharset, false);
    }
    catch (final ClassNotFoundException cle) {
      throw new RuntimeException(RUN_TOOL_CLASS_NAME + " must be on classpath for creating an embedded H2 database", cle);
    }
    catch (final InvocationTargetException ite) {
      if (ite.getCause() instanceof SQLException) {
        throw (SQLException) ite.getCause();
      }
      throw new RuntimeException(ite.getTargetException());
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void initializeEmbeddedDatabase(final List<String> scriptPaths) {
    synchronized (INITIALIZED_DATABASES) {
      final String url = getURL();
      if (!nullOrEmpty(scriptPaths) && (isEmbeddedInMemory() || !databaseFileExists()) && !INITIALIZED_DATABASES.contains(url.toLowerCase())) {
        final Properties properties = new Properties();
        properties.put(USER_PROPERTY, SYSADMIN_USERNAME);
        for (final String scriptPath : scriptPaths) {
          final String initUrl = getURL() + ";DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM '" + scriptPath.replace("\\", "/") + "'";
          try {
            DriverManager.getConnection(initUrl, properties).close();
            INITIALIZED_DATABASES.add(getURL().toLowerCase());
          }
          catch (final SQLException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private String getDatabasePath() {
    final String url = getURL();
    if (!url.toLowerCase().startsWith(URL_PREFIX_FILE)) {
      throw new IllegalStateException("Not a file based database (url prefix should be '" + JDBC_URL_PREFIX_FILE + "')");
    }
    String path = url.substring(URL_PREFIX_FILE.length());
    if (path.contains(";")) {
      path = path.substring(0, path.indexOf(";"));
    }

    return path;
  }

  private boolean isEmbeddedInMemory() {
    return getURL().startsWith(URL_PREFIX_MEM);
  }

  private boolean databaseFileExists() {
    return Files.exists(Paths.get(getDatabasePath() + FILE_SUFFIX_PAGESTORE)) ||
            Files.exists(Paths.get(getDatabasePath() + FILE_SUFFIX_MVSTORE));
  }
}
