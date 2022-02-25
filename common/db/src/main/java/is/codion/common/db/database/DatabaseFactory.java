/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Provides {@link Database} implementations
 */
public interface DatabaseFactory {

  /**
   * @param driverClassName the driver class name
   * @return true if this database provider is compatible with the given driver
   */
  boolean isDriverCompatible(String driverClassName);

  /**
   * @return a new {@link Database} implementation based on the given jdbc url.
   * @param jdbcUrl the jdbc url
   */
  Database createDatabase(String jdbcUrl);

  /**
   * @return a {@link DatabaseFactory} implementation for {@link Database#DATABASE_URL}
   * @throws IllegalStateException in case {@link Database#DATABASE_URL} ('codion.db.url') is not specified.
   * @throws IllegalArgumentException in case no such implementation is found
   * @throws SQLException in case loading of the database driver failed
   */
  static DatabaseFactory databaseFactory() throws SQLException {
    return databaseFactory(Database.DATABASE_URL.getOrThrow("codion.db.url must be specified before discovering DatabaseFactories"));
  }

  /**
   * @param jdbcUrl the jdbc url
   * @return a {@link DatabaseFactory} implementation for the given jdbc url
   * @throws IllegalArgumentException in case no such implementation is found
   * @throws SQLException in case loading of database driver failed
   */
  static DatabaseFactory databaseFactory(String jdbcUrl) throws SQLException {
    String driver = getDriverClassName(jdbcUrl);
    ServiceLoader<DatabaseFactory> loader = ServiceLoader.load(DatabaseFactory.class);
    for (DatabaseFactory factory : loader) {
      if (factory.isDriverCompatible(driver)) {
        return factory;
      }
    }

    throw new IllegalArgumentException("No DatabaseFactory implementation available for driver: " + driver);
  }

  /**
   * Returns a {@link Database} instance based on the currently configured JDBC URL ({@link Database#DATABASE_URL}).
   * Subsequent calls to this method return the same instance, until the JDBC URL changes, then a new instance is created.
   * @return a Database instance based on the current jdbc url
   * @see Database#DATABASE_URL
   * @throws IllegalArgumentException in case an unsupported database type is specified
   * @throws RuntimeException in case of an exception occurring while instantiating the database implementation
   */
  static Database getDatabase() {
    try {
      DatabaseFactory factory = databaseFactory();
      if (AbstractDatabase.instance == null || !AbstractDatabase.instance.getUrl().equals(Database.DATABASE_URL.get())) {
        //replace the instance
        AbstractDatabase.instance = factory.createDatabase(Database.DATABASE_URL.get());
      }

      return AbstractDatabase.instance;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param jdbcUrl the jdbc url
   * @return the database driver class name according to jdbc url
   * @throws SQLException in case loading of database driver failed
   */
  static String getDriverClassName(String jdbcUrl) throws SQLException {
    return DriverManager.getDriver(requireNonNull(jdbcUrl, "jdbcUrl")).getClass().getName();
  }
}
