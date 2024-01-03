/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;

/**
 * A class responsible for managing a local EntityConnection.
 * @see LocalEntityConnectionProvider#builder()
 */
public interface LocalEntityConnectionProvider extends EntityConnectionProvider {

  /**
   * @return the underlying domain model
   */
  Domain domain();

  /**
   * @return the underlying {@link Database} instance
   */
  Database database();

  /**
   * @return the default query timeout being used
   */
  int defaultQueryTimeout();

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
     * @param domain the domain model to base this connection on
     * @return this builder instance
     */
    Builder domain(Domain domain);

    /**
     * @param defaultQueryTimeout the default query timeout
     * @return this builder instance
     */
    Builder defaultQueryTimeout(int defaultQueryTimeout);
  }
}