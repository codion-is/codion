/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;

import java.util.Collection;

/**
 * A class encapsulating select query parameters.
 */
public interface SelectCondition extends Condition {

  int DEFAULT_QUERY_TIMEOUT_SECONDS = 120;

  /**
   * @return the underlying condition
   */
  Condition getCondition();

  /**
   * @return the OrderBy for this condition, null if none is specified
   */
  OrderBy getOrderBy();

  /**
   * @return the limit to use for the given condition, -1 for no limit
   */
  int getLimit();

  /**
   * @return the offset to use for the given condition, -1 for no offset
   */
  int getOffset();

  /**
   * @return true if this select should lock the result for update
   */
  boolean isForUpdate();

  /**
   * @return the query timeout
   */
  int getQueryTimeout();

  /**
   * @return the global fetch depth limit for this condition, null if none has been specified
   */
  Integer getFetchDepth();

  /**
   * Returns the number of levels of foreign key values to fetch, with 0 meaning no referenced entities
   * should be fetched, -1 no limit and null unspecified (use default).
   * @param foreignKey the foreign key
   * @return the number of levels of foreign key values to fetch
   */
  Integer getFetchDepth(ForeignKey foreignKey);

  /**
   * @return the attributes to include in the query result,
   * an empty Collection if all should be included
   */
  Collection<Attribute<?>> getSelectAttributes();

  /**
   * Sets the OrderBy for this condition
   * @param orderBy the OrderBy to use when applying this condition
   * @return a new SelectCondition instance with the given order by
   */
  SelectCondition orderBy(OrderBy orderBy);

  /**
   * @param limit the limit to use for this condition
   * @return a new SelectCondition instance with the given limit
   */
  SelectCondition limit(int limit);

  /**
   * @param offset the offset to use for this condition
   * @return a new SelectCondition instance with the given offset
   */
  SelectCondition offset(int offset);

  /**
   * Marks this condition as a select for update query, this means the resulting records
   * will be locked by the given connection until unlocked by running another (non - select for update)
   * query on the same connection or performing an update
   * @return a new SelectCondition instance with for update enabled
   */
  SelectCondition forUpdate();

  /**
   * Limit the levels of foreign keys to fetch
   * @param fetchDepth the foreign key fetch depth limit
   * @return a new SelectCondition instance with the given fetch depth
   */
  SelectCondition fetchDepth(int fetchDepth);

  /**
   * Limit the levels of foreign keys to fetch via the given foreign key
   * @param foreignKey the foreign key
   * @param fetchDepth the foreign key fetch depth limit
   * @return this SelectCondition instance
   */
  SelectCondition fetchDepth(ForeignKey foreignKey, int fetchDepth);

  /**
   * Sets the attributes to include in the query result. An empty array means all attributes should be included.
   * Note that primary key attribute values are always included.
   * Note that these must be ColumnProperty attributes
   * @param attributes the attributes to include
   * @return a new SelectCondition instance with the given select attributes
   */
  SelectCondition selectAttributes(Attribute<?>... attributes);

  /**
   * Sets the attributes to include in the query result. An empty Collection means all attributes should be included.
   * Note that primary key attribute values are always included.
   * @param attributes the attributes to include
   * @return a new SelectCondition instance with the given select attributes
   */
  SelectCondition selectAttributes(Collection<Attribute<?>> attributes);

  /**
   * @param queryTimeout the query timeout, 0 for no timeout
   * @return a new SelectCondition instance with the given query timeout
   */
  SelectCondition queryTimeout(int queryTimeout);
}
