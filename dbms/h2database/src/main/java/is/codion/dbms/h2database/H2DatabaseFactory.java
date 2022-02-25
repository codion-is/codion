/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.h2database;

import is.codion.common.Text;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.sql.SQLException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Provides h2 database implementations
 */
public final class H2DatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "org.h2";
  private static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
  private static final String SYSADMIN_USERNAME = "sa";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(final String jdbcUrl) {
    return new H2Database(jdbcUrl, Text.parseCommaSeparatedValues(Database.DATABASE_INIT_SCRIPTS.get()),
            Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }

  public static Database createDatabase(final String jdbcUrl, final String initScript) {
    return new H2Database(jdbcUrl, initScript == null ? emptyList() : singletonList(initScript),
            Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }

  /**
   * Runs the given script using the RunScript tool, with the default sysadmin username (sa) and default charset
   * @param database the database
   * @param scriptPath the path to the script
   * @throws SQLException in case of an exception
   */
  public static void runScript(final Database database, final String scriptPath) throws SQLException {
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
  public static void runScript(final Database database, final String scriptPath, final String username, final String password, final Charset scriptCharset) throws SQLException {
    try {
      Class runScriptToolClass = Class.forName(RUN_TOOL_CLASS_NAME);
      Method execute = runScriptToolClass.getMethod("execute", String.class, String.class, String.class, String.class, Charset.class, boolean.class);
      execute.invoke(runScriptToolClass.getDeclaredConstructor().newInstance(), database.getUrl(), username, password, scriptPath, scriptCharset, false);
    }
    catch (ClassNotFoundException cle) {
      throw new RuntimeException(RUN_TOOL_CLASS_NAME + " must be on classpath for creating an embedded H2 database", cle);
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
