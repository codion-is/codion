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
   * @param driverClassName the driver class name
   * @return true if this database provider is compatible with the given driver
   */
  boolean isDriverCompatible(String driverClassName);

  /**
   * @param database the database
   * @return true if the given database is compatible with this database provider
   */
  boolean isDatabaseCompatible(Database database);

  /**
   * @return a new {@link Database} implementation based on the given jdbc url.
   * @param jdbcUrl the jdbc url
   */
  Database createDatabase(String jdbcUrl);

  /**
   * @return a {@link DatabaseProvider} implementation for {@link Database#DATABASE_URL}
   * @throws IllegalArgumentException in case no such implementation is found
   * @throws SQLException in case loading of the database driver failed
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
    final String driver = getDriverClassName(jdbcUrl);
    final ServiceLoader<DatabaseProvider> loader = ServiceLoader.load(DatabaseProvider.class);
    for (final DatabaseProvider provider : loader) {
      if (provider.isDriverCompatible(driver)) {
        return provider;
      }
    }

    throw new IllegalArgumentException("No DatabaseProvider implementation available for driver: " + driver);
  }

  /**
   * @param jdbcUrl the jdbc url
   * @return the database driver class name according to jdbc url
   * @throws SQLException in case loading of database driver failed
   */
  static String getDriverClassName(final String jdbcUrl) throws SQLException {
    return DriverManager.getDriver(requireNonNull(jdbcUrl, "jdbcUrl")).getClass().getName();
  }
}
