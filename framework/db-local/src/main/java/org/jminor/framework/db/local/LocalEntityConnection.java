/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.Configuration;
import org.jminor.common.MethodLogger;
import org.jminor.common.Value;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.ResultIterator;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.condition.EntityCondition;
import org.jminor.framework.domain.Entity;

/**
 * EntityConnection implementation based on a local JDBC connection.
 * <pre>
 * Domain domain = new Domain();
 * EntityConditions conditions = new EntityConditions(domain);
 * Database database = new H2Database("pathToDb");
 * User user = new User("scott", "tiger".toCharArray());
 *
 * EntityConnection connection = LocalEntityConnections.createConnection(domain, database, user);
 *
 * List&lt;Entity&gt; entities = connection.selectMany(conditions.selectCondition(Domain.ENTITY_ID));
 *
 * connection.disconnect();
 * </pre>
 */
public interface LocalEntityConnection extends EntityConnection {

  int DEFAULT_CONNECTION_LOG_SIZE = 40;

  /**
   * Specifies the size of the (circular) log that is kept in memory for each connection<br>
   * Value type: Integer<br>
   * Default value: 40
   */
  Value<Integer> CONNECTION_LOG_SIZE = Configuration.integerValue("jminor.db.clientLogSize", DEFAULT_CONNECTION_LOG_SIZE);

  /**
   * Specifies whether optimistic locking should be performed, that is, if entities should
   * be selected for update and checked for modification before being updated<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  Value<Boolean> USE_OPTIMISTIC_LOCKING = Configuration.booleanValue("jminor.db.useOptimisticLocking", true);

  /**
   * Specifies whether the foreign key value graph should be fully populated instead of
   * being limited by the foreign key fetch depth setting.<br>
   * Value type: Boolean<br>
   * Default value: true<br>
   */
  Value<Boolean> LIMIT_FOREIGN_KEY_FETCH_DEPTH = Configuration.booleanValue("jminor.db.limitForeignKeyFetchDepth", true);

  /**
   * @param methodLogger the MethodLogger to use
   */
  void setMethodLogger(final MethodLogger methodLogger);

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
  ResultIterator<Entity> iterator(final EntityCondition condition) throws DatabaseException;

  /**
   * @return true if optimistic locking is enabled
   */
  boolean isOptimisticLocking();

  /**
   * @param optimisticLocking true if optimistic locking should be enabled
   */
  void setOptimisticLocking(final boolean optimisticLocking);

  /**
   * @return true if foreign key fetch depths are being limited
   */
  boolean isLimitForeignKeyFetchDepth();

  /**
   * @param limitForeignKeyFetchDepth false to override the fetch depth limit provided by condition
   * @see org.jminor.framework.db.condition.EntitySelectCondition#setForeignKeyFetchDepthLimit(int)
   */
  void setLimitForeignKeyFetchDepth(final boolean limitForeignKeyFetchDepth);
}
