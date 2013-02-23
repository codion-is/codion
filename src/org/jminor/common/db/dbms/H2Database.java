/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;
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
   * Specifies whether or not the database should be run in in-memory mode<br>
   * Values: "true"/"false"<br>
   * Default: "false"<br>
   */
  public static final String DATABASE_IN_MEMORY = "jminor.db.embeddedInMemory";
  /**
   * The script to run when initializing an embedded database
   */
  public static final String DATABASE_INIT_SCRIPT = "jminor.db.initScript";

  static final String DRIVER_CLASS_NAME = "org.h2.Driver";
  static final String AUTO_INCREMENT_QUERY = "CALL IDENTITY()";
  static final String SEQUENCE_VALUE_QUERY = "select next value for ";
  static final String SYSADMIN_USERNAME = "sa";
  static final String RUN_TOOL_CLASSNAME = "org.h2.tools.RunScript";
  static final String TRUE = "true";
  static final String FALSE = "false";
  static final boolean EMBEDDED_IN_MEMORY = TRUE.equals(System.getProperty(DATABASE_IN_MEMORY, FALSE));
  static final String URL_PREFIX = "jdbc:h2:";
  static final String URL_PREFIX_MEM = "jdbc:h2:mem:";

  static {
    loadDriver(DRIVER_CLASS_NAME);
    if (EMBEDDED_IN_MEMORY) {
      try {
        new H2Database().initializeMemoryDatabase(System.getProperty(DATABASE_INIT_SCRIPT));
      }
      catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final boolean embeddedInMemory;
  private String urlAppend = "";

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
   */
  public H2Database(final String databaseName, final boolean embeddedInMemory) {
    super(H2, databaseName, null, null, true);
    this.embeddedInMemory = embeddedInMemory;
  }

  /**
   * Instantiates a new networked H2Database.
   * @param host the host name
   * @param port the port number
   * @param databaseName the database name
   */
  public H2Database(final String host, final String port, final String databaseName) {
    super(H2, host, port, databaseName, false);
    this.embeddedInMemory = false;
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
   * Initializes a new H2 database, with the given script
   * @param scriptPath the path to the initialization script
   * @throws java.sql.SQLException in case of an exception
   */
  public void initializeMemoryDatabase(final String scriptPath) throws SQLException {
    if (!embeddedInMemory) {
      throw new IllegalArgumentException("Database is not configured as in memory database");
    }
    final Properties properties = new Properties();
    properties.put(USER_PROPERTY, SYSADMIN_USERNAME);
    String initializerString = ";DB_CLOSE_DELAY=-1";
    if (scriptPath != null) {
      initializerString += ";INIT=RUNSCRIPT FROM '" + scriptPath + "'";
    }
    DriverManager.getConnection(getURL(properties) + initializerString).close();
  }

  public void runScript(final String scriptPath) throws SQLException {
    try {
      final Class runScriptToolClass = Class.forName(RUN_TOOL_CLASSNAME);
      final Method execute = runScriptToolClass.getMethod("execute", String.class, String.class, String.class, String.class, String.class, boolean.class);
      execute.invoke(runScriptToolClass.newInstance(), getURL(null), SYSADMIN_USERNAME, "", scriptPath, null, false);
    }
    catch (ClassNotFoundException cle) {
      throw new RuntimeException(RUN_TOOL_CLASSNAME + " must be on classpath for creating an embedded H2 database", cle);
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof SQLException) {
        throw (SQLException) ite.getCause();
      }
      throw new RuntimeException(ite.getTargetException());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
