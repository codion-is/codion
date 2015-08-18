/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.db.Database;
import org.jminor.common.model.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A Database implementation based on the H2 database.
 */
public final class H2Database extends AbstractDatabase {
  /**
   * The error code representing incorrect login credentials
   */
  private static final int AUTHENTICATION_ERROR = 28000;

  static final String DRIVER_CLASS_NAME = "org.h2.Driver";
  static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String SYSADMIN_USERNAME = "sa";
  static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
  static final boolean EMBEDDED_IN_MEMORY = Boolean.TRUE.toString().equals(System.getProperty(DATABASE_EMBEDDED_IN_MEMORY, Boolean.FALSE.toString()));
  static final String URL_PREFIX = "jdbc:h2:";
  static final String URL_PREFIX_MEM = "jdbc:h2:mem:";

  private final boolean embeddedInMemory;
  private String urlAppend = "";

  static {
    //todo is this the right thing to do?
    if (System.getProperty(Database.DATABASE_TYPE, "").equals(Database.H2) && EMBEDDED_IN_MEMORY) {
      try {
        initializeMemoryDatabase(URL_PREFIX_MEM + System.getProperty(DATABASE_HOST) + ";user=" + SYSADMIN_USERNAME,
                System.getProperty(DATABASE_INIT_SCRIPT));
      }
      catch (final SQLException e) {
        throw new RuntimeException("Exception while initializing H2 memory database", e);
      }
    }
  }

  /**
   * Instantiates a new H2Database.
   */
  public H2Database() {
    this(System.getProperty(DATABASE_HOST));
  }

  /**
   * Instantiates a new file-based embedded H2Database.
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
    super(H2, DRIVER_CLASS_NAME, databaseName, null, null, true);
    this.embeddedInMemory = embeddedInMemory;
  }

  /**
   * Instantiates a new networked H2Database.
   * @param host the host name
   * @param port the port number
   * @param databaseName the database name
   */
  public H2Database(final String host, final String port, final String databaseName) {
    super(H2, DRIVER_CLASS_NAME, host, port, databaseName, false);
    this.embeddedInMemory = false;
  }

  /**
   * Instantiates a new H2Database instance, embedded in memory, based on the given script
   * @param databaseName the database name
   * @param initScript the script to use for initializing the database
   * @throws SQLException in case of an error during initialization
   */
  public H2Database(final String databaseName, final String initScript) throws SQLException {
    super(H2, DRIVER_CLASS_NAME, databaseName, null, null, true);
    this.embeddedInMemory = true;
    initializeMemoryDatabase(URL_PREFIX_MEM + databaseName + ";user=" + SYSADMIN_USERNAME, initScript);
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
    Util.rejectNullValue(sequenceName, "sequenceName");
    return SEQUENCE_VALUE_QUERY + sequenceName;
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
      final String urlPrefix = embeddedInMemory ? URL_PREFIX_MEM : URL_PREFIX;

      return urlPrefix + getHost() + (authentication == null ? "" : ";" + authentication) + urlAppend;
    }
    else {
      Util.require("host", getHost());
      Util.require("port", getPort());
      Util.require("sid", getSid());
      return URL_PREFIX + "//" + getHost() + ":" + getPort() + "/" + getSid() + (authentication == null ? "" : ";" + authentication) + urlAppend;
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
   * Runs the given script using the RunScript tool
   * @param scriptPath the path to the script
   * @throws SQLException in case of an exception
   */
  public void runScript(final String scriptPath) throws SQLException {
    try {
      final Class<?> runScriptToolClass = Class.forName(RUN_TOOL_CLASS_NAME);
      final Method execute = runScriptToolClass.getMethod("execute", String.class, String.class, String.class, String.class, String.class, boolean.class);
      execute.invoke(runScriptToolClass.newInstance(), getURL(null), SYSADMIN_USERNAME, "", scriptPath, null, false);
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
   * Initializes a new H2 database, with the given script
   * @param url the database url
   * @param scriptPath the path to the initialization script
   * @throws java.sql.SQLException in case of an exception
   */
  private static void initializeMemoryDatabase(final String url, final String scriptPath) throws SQLException {
    final Properties properties = new Properties();
    properties.put(USER_PROPERTY, SYSADMIN_USERNAME);
    String initializerString = ";DB_CLOSE_DELAY=-1";
    if (scriptPath != null) {
      initializerString += ";INIT=RUNSCRIPT FROM '" + scriptPath + "'";
    }
    DriverManager.getConnection(url + initializerString).close();
  }
}
