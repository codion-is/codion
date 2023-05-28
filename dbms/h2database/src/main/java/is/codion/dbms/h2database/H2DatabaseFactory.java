/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.h2database;

import is.codion.common.Text;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Provides h2 database implementations
 */
public final class H2DatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "org.h2";
  private static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
  private static final String SYSADMIN_USERNAME = "sa";

  @Override
  public boolean isDriverCompatible(String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String url) {
    return new H2Database(url, Text.parseCommaSeparatedValues(Database.DATABASE_INIT_SCRIPTS.get()),
            Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }

  /**
   * Creates a H2 Database instance
   * @param url the jdbc url
   * @param initScripts initialization scripts to run on database creation
   * @return a H2 Database instance
   */
  public static Database createDatabase(String url, String... initScripts) {
    return new H2Database(url, initScripts == null ? emptyList() : Arrays.asList(initScripts),
            Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }

  /**
   * Runs the given script using the RunScript tool, with the default sysadmin username (sa) and default charset
   * @param database the database
   * @param scriptPath the path to the script
   * @throws SQLException in case of an exception
   */
  public static void runScript(Database database, String scriptPath) throws SQLException {
    runScript(database, scriptPath, SYSADMIN_USERNAME, "", Charset.defaultCharset());
  }

  /**
   * Runs the given script using the RunScript tool
   * @param database the database
   * @param scriptPath the path to the script
   * @param username the username to run the script under
   * @param password the password
   * @param scriptCharset the script character set
   * @throws SQLException in case of an exception
   */
  public static void runScript(Database database, String scriptPath, String username, String password, Charset scriptCharset) throws SQLException {
    try {
      Class<?> runScriptToolClass = Class.forName(RUN_TOOL_CLASS_NAME);
      Method execute = runScriptToolClass.getMethod("execute", String.class, String.class, String.class, String.class, Charset.class, boolean.class);
      execute.invoke(runScriptToolClass.getDeclaredConstructor().newInstance(), database.url(), username, password, scriptPath, scriptCharset, false);
    }
    catch (ClassNotFoundException cle) {
      throw new RuntimeException(RUN_TOOL_CLASS_NAME + " must be on classpath for creating an embedded H2 database", cle);
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof SQLException) {
        throw (SQLException) ite.getCause();
      }
      throw new RuntimeException(ite.getCause());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
