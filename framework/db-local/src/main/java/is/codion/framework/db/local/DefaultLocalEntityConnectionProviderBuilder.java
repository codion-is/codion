/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.AbstractEntityConnectionProvider.AbstractBuilder;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;

import static java.util.Objects.requireNonNull;

/**
 * Builds a {@link LocalEntityConnectionProvider} instance.
 * @see LocalEntityConnectionProvider#builder()
 */
public final class DefaultLocalEntityConnectionProviderBuilder
        extends AbstractBuilder<LocalEntityConnectionProvider, LocalEntityConnectionProvider.Builder>
        implements LocalEntityConnectionProvider.Builder {

  Domain domain;
  Database database;
  int defaultQueryTimeout = LocalEntityConnection.QUERY_TIMEOUT_SECONDS.get();

  public DefaultLocalEntityConnectionProviderBuilder() {
    super(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
  }

  @Override
  public LocalEntityConnectionProvider.Builder database(Database database) {
    this.database = requireNonNull(database);
    return this;
  }

  @Override
  public LocalEntityConnectionProvider.Builder domain(Domain domain) {
    this.domain = requireNonNull(domain);
    return domainClassName(domain.getClass().getName());
  }

  @Override
  public LocalEntityConnectionProvider.Builder defaultQueryTimeout(int defaultQueryTimeout) {
    this.defaultQueryTimeout = defaultQueryTimeout;
    return this;
  }

  @Override
  public LocalEntityConnectionProvider build() {
    return new DefaultLocalEntityConnectionProvider(this);
  }
}
