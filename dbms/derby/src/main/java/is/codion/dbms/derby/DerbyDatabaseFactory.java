/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.derby;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import java.sql.DriverManager;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * Provides derby database implementations
 */
public final class DerbyDatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "org.apache.derby.jdbc";
  private static final String SHUTDOWN_ERROR_CODE = "08006";

  @Override
  public boolean driverCompatible(String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String url) {
    return new DerbyDatabase(url);
  }

  /**
   * Shuts down the given database instance, assuming it is embedded
   * @param database the database to shutdown
   */
  public static void shutdown(Database database) {
    requireNonNull(database);
    try {
      DriverManager.getConnection(database.url() + ";shutdown=true").close();
    }
    catch (SQLException e) {
      if (!e.getSQLState().equals(SHUTDOWN_ERROR_CODE)) {//08006 is expected on Derby shutdown
        System.err.println("Embedded Derby database did not successfully shut down: " + e.getMessage());
      }
    }
  }
}
