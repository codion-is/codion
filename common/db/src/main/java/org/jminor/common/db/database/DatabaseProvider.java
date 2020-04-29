/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ServiceLoader;

/**
 * Provides {@link Database} implementations
 */
public interface DatabaseProvider {

  /**
   * @param driverClass the driver class
   * @return true if this database provider fits the driver
   */
  boolean isCompatibleWith(String driverClass);

  /**
   * @return a new Database implementation based on configuration values
   */
  Database createDatabase();

  /**
   * @return a {@link DatabaseProvider} implementation for the {@link Database#getDatabaseType()} type
   * @throws IllegalArgumentException in case no such implementation is found
   */
  static DatabaseProvider getInstance() throws SQLException {
    final String jdbcUrl = Database.DATABASE_URL.get();
    if (jdbcUrl == null) {
      throw new IllegalStateException("jminor.db.url must be specified before instantiating a DatabaseProvider");
    }
    final Driver driver = DriverManager.getDriver(jdbcUrl);
    final ServiceLoader<DatabaseProvider> loader = ServiceLoader.load(DatabaseProvider.class);
    for (final DatabaseProvider provider : loader) {
      if (provider.isCompatibleWith(driver.getClass().getName())) {
        return provider;
      }
    }

    throw new IllegalArgumentException("No DatabaseProvider implementation available for driver: " + driver.getClass());
  }
}
