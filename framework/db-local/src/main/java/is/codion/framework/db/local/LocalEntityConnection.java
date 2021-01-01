/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.Configuration;
import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.logging.MethodLogger;
import is.codion.common.value.PropertyValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;

/**
 * EntityConnection implementation based on a local JDBC connection.
 * <pre>
 * Domain domain = new Domain();
 * Database database = new H2DatabaseFactory().createDatabase("jdbc:h2:file:/path/to/database");
 * User user = Users.parseUser("scott:tiger");
 *
 * EntityConnection connection = LocalEntityConnections.createConnection(domain, database, user);
 *
 * List&lt;Entity&gt; entities = connection.select(Conditions.condition(Domain.ENTITY_TYPE));
 *
 * connection.close();
 * </pre>
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
   * @return the underlying domain model
   */
  Domain getDomain();
}
