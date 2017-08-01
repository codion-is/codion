/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.TextUtil;
import org.jminor.common.Util;
import org.jminor.common.db.AbstractDatabase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * A Database implementation based on the H2 database.
 */
public final class H2Database extends AbstractDatabase {
  /**
   * The error code representing incorrect login credentials
   */
  private static final int AUTHENTICATION_ERROR = 28000;

  private static boolean sharedDatabaseInitialized = false;

  static final String DRIVER_CLASS_NAME = "org.h2.Driver";
  static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String SYSADMIN_USERNAME = "sa";
  static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
  static final boolean EMBEDDED_IN_MEMORY = Boolean.TRUE.toString().equals(System.getProperty(DATABASE_EMBEDDED_IN_MEMORY, Boolean.FALSE.toString()));
  static final String URL_PREFIX_SERVER = "jdbc:h2:";
  static final String URL_PREFIX_MEM = "jdbc:h2:mem:";
  static final String URL_PREFIX_FILE = "jdbc:h2:file:";

  private final boolean embeddedInMemory;
  private String urlAppend = "";

  /**
   * Instantiates a new H2Database.
   */
  public H2Database() {
    this(System.getProperty(DATABASE_HOST));
  }

  /**
   * Instantiates a new embedded H2Database.
   * @param databaseName the path to the database files
   */
  public H2Database(final String databaseName) {
    this(databaseName, EMBEDDED_IN_MEMORY);
  }

  /**
   * Instantiates a new embedded H2Database.
   * @param databaseName the path to the database files or the database name if in-memory
   * @param embeddedInMemory if true then this instance is memory based
   */
  public H2Database(final String databaseName, final boolean embeddedInMemory) {
    super(Type.H2, DRIVER_CLASS_NAME, databaseName, null, null, true);
    initializeSharedDatabase(databaseName, TextUtil.parseCommaSeparatedValues(System.getProperty(DATABASE_INIT_SCRIPT)), embeddedInMemory);
    this.embeddedInMemory = embeddedInMemory;
  }

  /**
   * Instantiates a new networked H2Database.
   * @param host the host name
   * @param port the port number
   * @param databaseName the database name
   */
  public H2Database(final String host, final String port, final String databaseName) {
    super(Type.H2, DRIVER_CLASS_NAME, host, port, databaseName, false);
    this.embeddedInMemory = false;
  }

  /**
   * Instantiates a new embedded H2Database instance, in memory, initialized with the given script, if any
   * @param databaseName the database name
   * @param initScript the script to use for initializing the database
   * @throws RuntimeException in case of an error during initialization
   */
  public H2Database(final String databaseName, final String initScript) {
    this(databaseName, initScript, true);
  }

  /**
   * Instantiates a new embedded H2Database instance, initialized with the given script, if any
   * @param databaseName the database name
   * @param initScript the script to use for initializing the database
   * @param embeddedInMemory if true then the resulting database is in memory
   * @throws RuntimeException in case of an error during initialization
   */
  public H2Database(final String databaseName, final String initScript, final boolean embeddedInMemory) {
    super(Type.H2, DRIVER_CLASS_NAME, databaseName, null, null, true);
    initializeDatabase(databaseName, Collections.singletonList(initScript), embeddedInMemory);
    this.embeddedInMemory = embeddedInMemory;

  }

  /**
   * @param urlAppend a string to append to the connection URL
   * @return this H2Database instance
   */
  public H2Database setUrlAppend(final String urlAppend) {
    this.urlAppend = urlAppend;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementValueSQL(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getSequenceSQL(final String sequenceName) {
    return SEQUENCE_VALUE_QUERY + Objects.requireNonNull(sequenceName, "sequenceName");
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      Util.require("host", getHost());
      if (connectionProperties != null && (Util.nullOrEmpty((String) connectionProperties.get(USER_PROPERTY)))) {
        connectionProperties.put(USER_PROPERTY, SYSADMIN_USERNAME);
      }
      final String urlPrefix = embeddedInMemory ? URL_PREFIX_MEM : URL_PREFIX_FILE;

      return urlPrefix + getHost() + (authentication == null ? "" : ";" + authentication) + urlAppend;
    }
    else {
      Util.require("host", getHost());
      Util.require("port", getPort());
      Util.require("sid", getSid());
      return URL_PREFIX_SERVER + "//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication) + urlAppend;
    }
  }

  /**
   * @param exception the exception
   * @return true if this exception represents a login credentials failure
   */
  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getErrorCode() == AUTHENTICATION_ERROR;
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
   * @param scriptCharset the script characterset
   * @throws SQLException in case of an exception
   */
  public void runScript(final String scriptPath, final String username, final String password, final Charset scriptCharset) throws SQLException {
    try {
      final Class<?> runScriptToolClass = Class.forName(RUN_TOOL_CLASS_NAME);
      final Method execute = runScriptToolClass.getMethod("execute", String.class, String.class, String.class, String.class, Charset.class, boolean.class);
      execute.invoke(runScriptToolClass.newInstance(), getURL(null), username, password, scriptPath, scriptCharset, false);
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
   * Initializes a shared H2 database instance
   */
  private static synchronized void initializeSharedDatabase(final String databaseName, final List<String> scriptPaths,
                                                            final boolean inMemory) {
    if (!sharedDatabaseInitialized) {
      initializeDatabase(databaseName, scriptPaths, inMemory);
      sharedDatabaseInitialized = true;
    }
  }

  private static void initializeDatabase(final String databaseName, final List<String> scriptPaths, final boolean inMemory) {
    if (!Util.nullOrEmpty(scriptPaths) && (inMemory || !Files.exists(Paths.get(databaseName + ".h2.db")))) {
      final Properties properties = new Properties();
      properties.put(USER_PROPERTY, SYSADMIN_USERNAME);
      for (final String scriptPath : scriptPaths) {
        final String url = (inMemory ? URL_PREFIX_MEM : URL_PREFIX_FILE) + databaseName
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
}
