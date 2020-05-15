/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.database;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for {@link Database} implementations and misc. database related things.
 */
public final class Databases {

  private static Database instance;

  private Databases() {}

  /**
   * @return a Database instance based on the current jdbc url
   * @see Database#DATABASE_URL
   * @throws IllegalArgumentException in case an unsupported database type is specified
   * @throws RuntimeException in case of an exception occurring while instantiating the database implementation
   */
  public static synchronized Database getInstance() {
    try {
      final DatabaseProvider provider = DatabaseProvider.getInstance();
      if (instance == null || !provider.isDatabaseCompatible(instance)) {
        //refresh the instance
        instance = provider.createDatabase(requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty()));
      }

      return instance;
    }
    catch (final RuntimeException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}