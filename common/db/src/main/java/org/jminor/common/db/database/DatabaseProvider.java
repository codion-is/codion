/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

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
   * @return the Class of the Database this provider provides
   */
  Class<? extends Database> getDatabaseClass();

  /**
   * @return a new Database implementation based on configuration values
   */
  Database createDatabase();

  /**
   * @return a {@link DatabaseProvider} implementation for {@link Database#DATABASE_URL}
   * @throws IllegalArgumentException in case no such implementation is found
   * @throws SQLException in case loading of database driver failed
   */
  static DatabaseProvider getInstance() throws SQLException {
    final String jdbcUrl = Database.DATABASE_URL.get();
    if (jdbcUrl == null) {
      throw new IllegalStateException("jminor.db.url must be specified before discovering DatabaseProviders");
    }

    return getInstance(jdbcUrl);
  }

  /**
   * @param jdbcUrl the jdbc url
   * @return a {@link DatabaseProvider} implementation for the given jdbc url
   * @throws IllegalArgumentException in case no such implementation is found
   * @throws SQLException in case loading of database driver failed
   */
  static DatabaseProvider getInstance(final String jdbcUrl) throws SQLException {
    final String driver = getDatabaseDriverClass(jdbcUrl);
    final ServiceLoader<DatabaseProvider> loader = ServiceLoader.load(DatabaseProvider.class);
    for (final DatabaseProvider provider : loader) {
      if (provider.isCompatibleWith(driver)) {
        return provider;
      }
    }

    throw new IllegalArgumentException("No DatabaseProvider implementation available for driver: " + driver.getClass());
  }

  /**
   * @param jdbcUrl the jdbc url
   * @return the database driver class discovered from jdbc url
   * @throws SQLException in case loading of database driver failed
   */
  static String getDatabaseDriverClass(final String jdbcUrl) throws SQLException {
    requireNonNull(jdbcUrl, "jdbcUrl");
    return DriverManager.getDriver(jdbcUrl).getClass().getName();
  }
}
