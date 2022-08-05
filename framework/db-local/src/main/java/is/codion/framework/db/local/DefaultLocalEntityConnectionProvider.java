/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;

/**
 * A class responsible for managing a local EntityConnection.
 * @see LocalEntityConnectionProvider#builder()
 */
final class DefaultLocalEntityConnectionProvider extends AbstractEntityConnectionProvider
        implements LocalEntityConnectionProvider {

  private static final Logger LOG = LoggerFactory.getLogger(LocalEntityConnectionProvider.class);

  private final boolean shutdownDatabaseOnDisconnect = SHUTDOWN_EMBEDDED_DB_ON_DISCONNECT.get();

  private final Domain domain;
  private final Database database;
  private final int defaultQueryTimeout;

  DefaultLocalEntityConnectionProvider(DefaultLocalEntityConnectionProviderBuilder builder) {
    super(builder);
    this.domain = initializeDomain(domainClassName());
    this.database = builder.database == null ? DatabaseFactory.getDatabase() : builder.database;
    this.defaultQueryTimeout = builder.defaultQueryTimeout;
  }

  @Override
  public String connectionType() {
    return CONNECTION_TYPE_LOCAL;
  }

  @Override
  public String description() {
    return database().getName().toUpperCase();
  }

  public Domain domain() {
    return domain;
  }

  public Database database() {
    return database;
  }

  public int defaultQueryTimeout() {
    return defaultQueryTimeout;
  }

  @Override
  protected LocalEntityConnection connect() {
    try {
      LOG.debug("Initializing connection for {}", user());
      return localEntityConnection(database(), domain(), user())
              .setDefaultQueryTimeout(defaultQueryTimeout);
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    connection.close();
    if (shutdownDatabaseOnDisconnect) {
      database.shutdownEmbedded();
    }
  }

  private static Domain initializeDomain(String domainClassName) {
    return Domain.getInstanceByClassName(domainClassName)
            .orElseGet(() -> createDomainInstance(domainClassName));
  }

  private static Domain createDomainInstance(String domainClassName) {
    LOG.debug("Domain of type " + domainClassName + " not found in services");
    try {
      return (Domain) Class.forName(domainClassName).getConstructor().newInstance();
    }
    catch (Exception e) {
      LOG.error("Error when instantiating Domain of type " + domainClassName);
      throw new RuntimeException(e);
    }
  }
}