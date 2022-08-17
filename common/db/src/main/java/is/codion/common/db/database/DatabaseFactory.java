/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Provides {@link Database} implementations
 * @see #instance()
 * @see #instance(String)
 * @see #createDatabase(String)
 */
public interface DatabaseFactory {

  /**
   * @param driverClassName the driver class name
   * @return true if this database factory is compatible with the given driver
   */
  boolean isDriverCompatible(String driverClassName);

  /**
   * @return a new {@link Database} implementation based on the given jdbc url.
   * @param url the jdbc url
   */
  Database createDatabase(String url);

  /**
   * @return a {@link DatabaseFactory} implementation for {@link Database#DATABASE_URL}
   * @throws IllegalStateException in case {@link Database#DATABASE_URL} ('codion.db.url') is not specified.
   * @throws IllegalArgumentException in case no such implementation is found
   * @throws SQLException in case loading of the database driver failed
   * @throws IllegalArgumentException in case no implementation exists for the configured jdbc url
   */
  static DatabaseFactory instance() throws SQLException {
    return instance(Database.DATABASE_URL.getOrThrow("codion.db.url must be specified before discovering DatabaseFactories"));
  }

  /**
   * @param url the jdbc url
   * @return a {@link DatabaseFactory} implementation for the given jdbc url
   * @throws IllegalArgumentException in case no such implementation is found
   * @throws SQLException in case loading of database driver failed
   * @throws IllegalArgumentException in case no implementation exists for the given jdbc url
   */
  static DatabaseFactory instance(String url) throws SQLException {
    String driver = driverClassName(url);
    ServiceLoader<DatabaseFactory> loader = ServiceLoader.load(DatabaseFactory.class);
    for (DatabaseFactory factory : loader) {
      if (factory.isDriverCompatible(driver)) {
        return factory;
      }
    }

    throw new IllegalArgumentException("No DatabaseFactory implementation available for driver: " + driver);
  }

  /**
   * @param url the jdbc url
   * @return the database driver class name according to jdbc url
   * @throws SQLException in case loading of database driver failed
   */
  static String driverClassName(String url) throws SQLException {
    return DriverManager.getDriver(requireNonNull(url, "url")).getClass().getName();
  }
}
