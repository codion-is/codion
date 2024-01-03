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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.Configuration;
import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;

import java.sql.Connection;

/**
 * EntityConnection implementation based on a local JDBC connection.
 * <pre>
 * Domain domain = new Domain();
 * Database database = new H2DatabaseFactory().createDatabase("jdbc:h2:file:/path/to/database");
 * User user = User.parse("scott:tiger");
 *
 * try (EntityConnection connection = LocalEntityConnection.localEntityConnection(database, domain, user)) {
 *   List&lt;Entity&gt; customers = connection.select(all(Customer.TYPE));
 * }
 * </pre>
 * A factory class for creating LocalEntityConnection instances.
 */
public interface LocalEntityConnection extends EntityConnection {

  int DEFAULT_CONNECTION_LOG_SIZE = 40;

  /**
   * Specifies the size of the (circular) log that is kept in memory for each connection<br>
   * Value type: Integer<br>
   * Default value: 40
   */
  PropertyValue<Integer> CONNECTION_LOG_SIZE = Configuration.integerValue("codion.db.connectionLogSize", DEFAULT_CONNECTION_LOG_SIZE);

  /**
   * Specifies the query timeout in seconds<br>
   * Value type: Integer<br>
   * Default value: 120
   */
  PropertyValue<Integer> QUERY_TIMEOUT_SECONDS = Configuration.integerValue("codion.db.queryTimeoutSeconds", DEFAULT_QUERY_TIMEOUT_SECONDS);

  /**
   * Specifies whether optimistic locking should be performed, that is, if entities should
   * be selected for update and checked for modification before being updated<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> OPTIMISTIC_LOCKING = Configuration.booleanValue("codion.db.optimisticLocking", true);

  /**
   * Specifies whether the foreign key value graph should be fully populated instead of
   * being limited by the foreign key fetch depth setting.<br>
   * Value type: Boolean<br>
   * Default value: true<br>
   */
  PropertyValue<Boolean> LIMIT_FOREIGN_KEY_FETCH_DEPTH = Configuration.booleanValue("codion.db.limitForeignKeyFetchDepth", true);

  /**
   * @return the underlying connection
   */
  DatabaseConnection databaseConnection();

  /**
   * Returns a result set iterator based on the given query condition.
   * Remember to use try with resources or to call {@link ResultIterator#close()} in order to close underlying resources.
   * @param condition the query condition
   * @return an iterator for the given query condition
   * @throws DatabaseException in case of an exception
   */
  ResultIterator<Entity> iterator(Condition condition) throws DatabaseException;

  /**
   * Returns a result set iterator based on the given select.
   * Remember to use try with resources or to call {@link ResultIterator#close()} in order to close underlying resources.
   * @param select the query select
   * @return an iterator for the given query select
   * @throws DatabaseException in case of an exception
   */
  ResultIterator<Entity> iterator(Select select) throws DatabaseException;

  /**
   * @return true if optimistic locking is enabled
   */
  boolean isOptimisticLocking();

  /**
   * @param optimisticLocking true if optimistic locking should be enabled
   */
  void setOptimisticLocking(boolean optimisticLocking);

  /**
   * @return true if foreign key fetch depths are being limited
   */
  boolean isLimitForeignKeyFetchDepth();

  /**
   * @param limitForeignKeyFetchDepth false to override the fetch depth limit specified by conditions or entities
   * @see Select.Builder#fetchDepth(int)
   */
  void setLimitForeignKeyFetchDepth(boolean limitForeignKeyFetchDepth);

  /**
   * @return the default query timeout being used
   */
  int getDefaultQueryTimeout();

  /**
   * @param queryTimeout the query timeout in seconds
   */
  void setDefaultQueryTimeout(int queryTimeout);

  /**
   * Constructs a new LocalEntityConnection instance
   * @param database the Database instance
   * @param domain the domain model
   * @param user the user used for connecting to the database
   * @return a new LocalEntityConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
   */
  static LocalEntityConnection localEntityConnection(Database database, Domain domain,
                                                     User user) throws DatabaseException {
    return new DefaultLocalEntityConnection(database, domain, user);
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param database the Database instance
   * @param domain the domain model
   * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
   * @return a new LocalEntityConnection instance, wrapping the given connection
   * @throws DatabaseException in case there is a problem with the supplied connection
   */
  static LocalEntityConnection localEntityConnection(Database database, Domain domain,
                                                     Connection connection) throws DatabaseException {
    return new DefaultLocalEntityConnection(database, domain, connection);
  }

  /**
   * Runs the database configuration for the given domain on the given database.
   * Prevents multiple runs for the same domain/database combination.
   * @param database the database to configure
   * @param domain the domain doing the configuring
   * @return the Database instance
   * @throws DatabaseException in case of an exception
   */
  static Database configureDatabase(Database database, Domain domain) throws DatabaseException {
    return DefaultLocalEntityConnection.configureDatabase(database, domain);
  }
}
