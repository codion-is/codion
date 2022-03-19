/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.Configuration;
import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.logging.MethodLogger;
import is.codion.common.properties.PropertyValue;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;

import java.sql.Connection;

/**
 * EntityConnection implementation based on a local JDBC connection.
 * <pre>
 * Domain domain = new Domain();
 * Database database = new H2DatabaseFactory().createDatabase("jdbc:h2:file:/path/to/database");
 * User user = User.parse("scott:tiger");
 *
 * EntityConnection connection = LocalEntityConnections.localEntityConnection(domain, database, user);
 *
 * List&lt;Entity&gt; entities = connection.select(Conditions.condition(Domain.ENTITY_TYPE));
 *
 * connection.close();
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
  PropertyValue<Integer> CONNECTION_LOG_SIZE = Configuration.integerValue("codion.db.clientLogSize", DEFAULT_CONNECTION_LOG_SIZE);

  /**
   * Specifies the query timeout in seconds<br>
   * Value type: Integer<br>
   * Default value: 120
   */
  PropertyValue<Integer> QUERY_TIMEOUT_SECONDS = Configuration.integerValue("codion.db.queryTimeoutSeconds", SelectCondition.DEFAULT_QUERY_TIMEOUT_SECONDS);

  /**
   * Specifies whether optimistic locking should be performed, that is, if entities should
   * be selected for update and checked for modification before being updated<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> USE_OPTIMISTIC_LOCKING = Configuration.booleanValue("codion.db.useOptimisticLocking", true);

  /**
   * Specifies whether the foreign key value graph should be fully populated instead of
   * being limited by the foreign key fetch depth setting.<br>
   * Value type: Boolean<br>
   * Default value: true<br>
   */
  PropertyValue<Boolean> LIMIT_FOREIGN_KEY_FETCH_DEPTH = Configuration.booleanValue("codion.db.limitForeignKeyFetchDepth", true);

  /**
   * @param methodLogger the MethodLogger to use
   * @return this LocalEntityConnection instance
   */
  EntityConnection setMethodLogger(MethodLogger methodLogger);

  /**
   * @return the MethodLogger being used
   */
  MethodLogger getMethodLogger();

  /**
   * @return the underlying connection
   */
  DatabaseConnection getDatabaseConnection();

  /**
   * Returns a result set iterator based on the given query condition, this iterator closes all underlying
   * resources in case of an exception and when it finishes iterating.
   * Calling {@link ResultIterator#close()} is required if the iterator has not been exhausted and is always recommended.
   * @param condition the query condition
   * @return an iterator for the given query condition
   * @throws DatabaseException in case of an exception
   */
  ResultIterator<Entity> iterator(Condition condition) throws DatabaseException;

  /**
   * @return true if optimistic locking is enabled
   */
  boolean isOptimisticLockingEnabled();

  /**
   * @param optimisticLocking true if optimistic locking should be enabled
   * @return this LocalEntityConnection instance
   */
  LocalEntityConnection setOptimisticLockingEnabled(boolean optimisticLocking);

  /**
   * @return true if foreign key fetch depths are being limited
   */
  boolean isLimitForeignKeyFetchDepth();

  /**
   * @param limitFetchDepth false to override the fetch depth limit provided by condition
   * @return this LocalEntityConnection instance
   * @see SelectCondition#fetchDepth(int)
   */
  LocalEntityConnection setLimitFetchDepth(boolean limitFetchDepth);

  /**
   * @return the default query timeout being used
   */
  int getDefaultQueryTimeout();

  /**
   * @param queryTimeout the query timeout in seconds
   * @return this LocalEntityConnection instance
   */
  LocalEntityConnection setDefaultQueryTimeout(int queryTimeout);

  /**
   * @return the underlying domain model
   */
  Domain getDomain();

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param user the user used for connecting to the database
   * @return a new LocalEntityConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
   */
  static LocalEntityConnection localEntityConnection(Domain domain, Database database,
                                                     User user) throws DatabaseException {
    return new DefaultLocalEntityConnection(domain, database, user);
  }

  /**
   * Constructs a new LocalEntityConnection instance
   * @param domain the domain model
   * @param database the Database instance
   * @param connection the connection object to base the entity connection on, it is assumed to be in a valid state
   * @return a new LocalEntityConnection instance, wrapping the given connection
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case a validation statement is required but could not be created
   * @see Database#supportsIsValid()
   */
  static LocalEntityConnection localEntityConnection(Domain domain, Database database,
                                                     Connection connection) throws DatabaseException {
    return new DefaultLocalEntityConnection(domain, database, connection);
  }
}
