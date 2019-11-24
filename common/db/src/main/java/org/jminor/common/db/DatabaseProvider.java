/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Provides {@link Database} implementations
 */
public interface DatabaseProvider {

  /**
   * @return the type of database this provider provides
   */
  Database.Type getDatabaseType();

  /**
   * @return a new Database implementation based on configuration values
   */
  Database createDatabase();

  /**
   * @return a {@link DatabaseProvider} implementation for the {@link Database#getDatabaseType()} type
   * @throws IllegalArgumentException in case no such implementation is found
   */
  static DatabaseProvider getInstance() {
    final Database.Type databaseType = Database.getDatabaseType();
    try {
      final ServiceLoader<DatabaseProvider> loader = ServiceLoader.load(DatabaseProvider.class);
      for (final DatabaseProvider provider : loader) {
        if (Objects.equals(provider.getDatabaseType(), databaseType)) {
          return provider;
        }
      }
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }

    throw new IllegalArgumentException("No DatabaseProvider implementation available for type: " + databaseType);
  }
}
