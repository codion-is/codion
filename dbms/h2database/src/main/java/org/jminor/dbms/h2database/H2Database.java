/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.h2database;

import org.jminor.common.Text;
import org.jminor.common.db.database.AbstractDatabase;
import org.jminor.common.db.database.Database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A Database implementation based on the H2 database.
 */
public final class H2Database extends AbstractDatabase {
  /**
   * The error code representing incorrect login credentials
   */
  private static final int AUTHENTICATION_ERROR = 28000;
  private static final int REFERENTIAL_INTEGRITY_ERROR_CHILD_EXISTS = 23503;
  private static final int REFERENTIAL_INTEGRITY_ERROR_PARENT_MISSING = 23506;
  private static final int UNIQUE_CONSTRAINT_ERROR = 23505;
  private static final String DEFAULT_EMBEDDED_DATABASE_NAME = "h2db";

  static final String DRIVER_CLASS_NAME = "org.h2.Driver";
  static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String SYSADMIN_USERNAME = "sa";
  static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
  static final String URL_PREFIX_SERVER = "jdbc:h2:";
  static final String URL_PREFIX_MEM = "jdbc:h2:mem:";
  static final String URL_PREFIX_FILE = "jdbc:h2:file:";

  private final boolean embeddedInMemory;

  /**
   * Instantiates a new embedded H2Database, using {@link Database#DATABASE_HOST} for database name.
   */
  H2Database() {
    this(Database.DATABASE_HOST.get());
  }

  /**
   * Instantiates a new embedded H2Database.
   * @param databaseName the path to the database files
   */
  private H2Database(final String databaseName) {
    this(databaseName, Database.DATABASE_EMBEDDED_IN_MEMORY.get(),
            Text.parseCommaSeparatedValues(Database.DATABASE_INIT_SCRIPT.get()));
  }

  /**
   * Instantiates a new embedded H2Database.
   * @param databaseName the path to the database files or the database name if in-memory
   * @param embeddedInMemory if true then this instance is memory based
   * @param scriptPaths paths to the scripts to run to initialize the database
   */
  private H2Database(final String databaseName, final boolean embeddedInMemory, final List<String> scriptPaths) {
    super(Type.H2, DRIVER_CLASS_NAME, getEmbeddedName(databaseName, embeddedInMemory), null, null, true);
    this.embeddedInMemory = embeddedInMemory;
    initializeEmbeddedDatabase(scriptPaths);
  }

  /**
   * Instantiates a new networked H2Database.
   * @param host the host name
   * @param port the port number
   * @param databaseName the database name
   */
  private H2Database(final String host, final Integer port, final String databaseName) {
    super(Type.H2, DRIVER_CLASS_NAME, requireNonNull(host, "host"), requireNonNull(port, "port"),
            requireNonNull(databaseName, "databaseName"), false);
    this.embeddedInMemory = false;
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
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      if (connectionProperties != null && (nullOrEmpty((String) connectionProperties.get(USER_PROPERTY)))) {
        connectionProperties.put(USER_PROPERTY, SYSADMIN_USERNAME);
      }
      final String urlPrefix = embeddedInMemory ? URL_PREFIX_MEM : URL_PREFIX_FILE;

      return urlPrefix + getHost() + (authentication == null ? "" : ";" + authentication) + getUrlAppend();
    }
    else {
      return URL_PREFIX_SERVER + "//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication) + getUrlAppend();
    }
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
      execute.invoke(runScriptToolClass.getDeclaredConstructor().newInstance(), getURL(null), username, password, scriptPath, scriptCharset, false);
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

  /**
   * Instantiates a new server-based database.
   * @param host the server host
   * @param port the server port
   * @param databaseName the database name
   * @return a server-based database
   */
  public static H2Database h2ServerDatabase(final String host, final Integer port, final String databaseName) {
    return new H2Database(host, port, databaseName);
  }

  /**
   * Instantiates a new file-based database.
   * @param databaseFile the path to the database file
   * @return a file-based database
   */
  public static H2Database h2FileDatabase(final String databaseFile) {
    return h2FileDatabase(databaseFile, emptyList());
  }

  /**
   * Instantiates a new file-based database, initialized with the given script.
   * @param databaseFile the path to the database file
   * @param scriptPath the initialization script path
   * @return a file-based database
   */
  public static H2Database h2FileDatabase(final String databaseFile, final String scriptPath) {
    return new H2Database(databaseFile, false, singletonList(scriptPath));
  }

  /**
   * Instantiates a new file-based database, initialized with the given scripts.
   * @param databaseFile the path to the database file
   * @param scriptPaths the initialization script paths
   * @return a file-based database
   */
  public static H2Database h2FileDatabase(final String databaseFile, final List<String> scriptPaths) {
    return new H2Database(databaseFile, false, scriptPaths);
  }

  /**
   * Instantiates a new empty in memory database.
   * @param databaseName the database name
   * @return a in memory database
   */
  public static H2Database h2MemoryDatabase(final String databaseName) {
    return h2MemoryDatabase(databaseName, emptyList());
  }

  /**
   * Instantiates a new in memory database, initialized with the given script.
   * @param databaseName the database name
   * @param scriptPath the initialization script path
   * @return a in memory database
   */
  public static H2Database h2MemoryDatabase(final String databaseName, final String scriptPath) {
    return new H2Database(databaseName, true, singletonList(scriptPath));
  }

  /**
   * Instantiates a new in memory database, initialized with the given scripts.
   * @param databaseName the database name
   * @param scriptPaths the initialization script paths
   * @return a in memory database
   */
  public static H2Database h2MemoryDatabase(final String databaseName, final List<String> scriptPaths) {
    return new H2Database(databaseName, true, scriptPaths);
  }

  private void initializeEmbeddedDatabase(final List<String> scriptPaths) {
    if (!nullOrEmpty(scriptPaths) && (embeddedInMemory || !Files.exists(Paths.get(getHost() + ".h2.db")))) {
      final Properties properties = new Properties();
      properties.put(USER_PROPERTY, SYSADMIN_USERNAME);
      for (final String scriptPath : scriptPaths) {
        final String url = (embeddedInMemory ? URL_PREFIX_MEM : URL_PREFIX_FILE) + getHost()
                + ";DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM '" + scriptPath.replace("\\", "/") + "'";
        try {
          DriverManager.getConnection(url, properties).close();
        }
        catch (final SQLException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Handle the case where {@link Database#DATABASE_HOST} is not specified for in-memory databases
   * @return DEFAULT_EMBEDDED_DATABASE_NAME in case this is an in-memory database and databaseName is null
   */
  private static String getEmbeddedName(final String databaseName, final boolean embeddedInMemory) {
    if (embeddedInMemory) {
      return databaseName == null ? DEFAULT_EMBEDDED_DATABASE_NAME : databaseName;
    }

    return databaseName;
  }
}
