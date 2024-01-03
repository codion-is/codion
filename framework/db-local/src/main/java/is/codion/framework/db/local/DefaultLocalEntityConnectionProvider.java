/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.AbstractEntityConnectionProvider;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;

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

  private final Domain domain;
  private final Database database;
  private final int defaultQueryTimeout;

  DefaultLocalEntityConnectionProvider(DefaultLocalEntityConnectionProviderBuilder builder) {
    super(builder);
    this.domain = builder.domain == null ? initializeDomain(domainType()) : builder.domain;
    this.database = builder.database == null ? Database.instance() : builder.database;
    this.defaultQueryTimeout = builder.defaultQueryTimeout;
  }

  @Override
  public String connectionType() {
    return CONNECTION_TYPE_LOCAL;
  }

  @Override
  public String description() {
    return database().name().toUpperCase();
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
      LocalEntityConnection connection = localEntityConnection(database(), domain(), user());
      connection.setDefaultQueryTimeout(defaultQueryTimeout);

      return connection;
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void close(EntityConnection connection) {
    connection.close();
  }

  private static Domain initializeDomain(DomainType domainType) {
    return Domain.domains().stream()
            .filter(domain -> domain.type().equals(domainType))
            .findAny()
            .orElseThrow(() -> new IllegalStateException("Domain model not found in ServiceLoader: " + domainType));
  }
}