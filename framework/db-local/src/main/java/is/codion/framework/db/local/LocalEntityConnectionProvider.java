/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.Configuration;
import is.codion.common.db.database.Database;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;

/**
 * A class responsible for managing a local EntityConnection.
 * @see LocalEntityConnectionProvider#builder()
 */
public interface LocalEntityConnectionProvider extends EntityConnectionProvider {

  /**
   * Specifies whether an embedded database is shut down when disconnected from<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT =
          Configuration.booleanValue("codion.db.shutdownEmbeddedOnDisconnect", false);

  /**
   * @return the underlying domain model
   */
  Domain getDomain();

  /**
   * @return the underlying {@link Database} instance
   */
  Database getDatabase();

  /**
   * @return the default query timeout being used
   */
  int getDefaultQueryTimeout();

  /**
   * Instantiates a new builder instance.
   * @return a new builder
   */
  static Builder builder() {
    return new DefaultLocalEntityConnectionProviderBuilder();
  }

  /**
   * Builds a {@link LocalEntityConnectionProvider}.
   */
  interface Builder extends EntityConnectionProvider.Builder<LocalEntityConnectionProvider, Builder> {

    /**
     * @param database the database instance to use
     * @return this builder instance
     */
    Builder database(Database database);

    /**
     * @param defaultQueryTimeout the default query timeout
     * @return this builder instance
     */
    Builder defaultQueryTimeout(int defaultQueryTimeout);
  }
}