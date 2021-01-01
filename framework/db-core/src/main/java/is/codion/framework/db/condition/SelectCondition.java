/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;

import java.util.List;

/**
 * A class encapsulating select query parameters.
 */
public interface SelectCondition extends Condition {

  /**
   * @return the underlying condition
   */
  Condition getCondition();

  /**
   * @return the OrderBy for this condition, null if none is specified
   */
  OrderBy getOrderBy();

  /**
   * Sets the OrderBy for this condition
   * @param orderBy the OrderBy to use when applying this condition
   * @return this SelectCondition instance
   */
  SelectCondition orderBy(OrderBy orderBy);

  /**
   * @return the limit to use for the given condition
   */
  int getLimit();

  /**
   * @param limit the limit to use for this condition
   * @return this SelectCondition instance
   */
  SelectCondition limit(int limit);

  /**
   * @return the offset to use for the given condition
   */
  int getOffset();

  /**
   * @param offset the offset to use for this condition
   * @return this SelectCondition instance
   */
  SelectCondition offset(int offset);

  /**
   * @return true if this select should lock the result for update
   */
  boolean isForUpdate();

  /**
   * Marks this condition as a select for update query, this means the resulting records
   * will be locked by the given connection until unlocked by running another (non - select for update)
   * query on the same connection or performing an update
   * @return this SelectCondition instance
   */
  SelectCondition forUpdate();

  /**
   * @return the maximum number of records to fetch from the result
   */
  int getFetchCount();

  /**
   * @param fetchCount the maximum number of records to fetch from the result
   * @return this SelectCondition instance
   */
  SelectCondition fetchCount(int fetchCount);

  /**
   * Returns the number of levels of foreign key values to fetch, with 0 meaning no referenced entities
   * should be fetched, -1 no limit and null unspecified (use default).
   * @param foreignKey the foreign key
   * @return the number of levels of foreign key values to fetch
   */
  Integer getFetchDepth(ForeignKey foreignKey);

  /**
   * Limit the levels of foreign keys to fetch via the given foreign key
   * @param foreignKey the foreign key
   * @param fetchDepth the foreign key fetch depth limit
   * @return this SelectCondition instance
   */
  SelectCondition fetchDepth(ForeignKey foreignKey, int fetchDepth);

  /**
   * @return the global fetch depth limit for this condition, null if none has been specified
   */
  Integer getFetchDepth();

  /**
   * Limit the levels of foreign keys to fetch
   * @param fetchDepth the foreign key fetch depth limit
   * @return this SelectCondition instance
   */
  SelectCondition fetchDepth(int fetchDepth);

  /**
   * Sets the attributes to include in the resulting Entities,
   * including the column attributes involved in a foreign key
   * causes the foreign key values to be populated.
   * If you want the primary key to be populated you must include
   * the primary key attributes.
   * Note that these must be ColumnProperty attributes
   * @param attributes the attributes to include
   * @return this SelectCondition instance
   */
  SelectCondition attributes(Attribute<?>... attributes);

  /**
   * @return the attributes to include in the query result,
   * an empty list if all should be included
   */
  List<Attribute<?>> getSelectAttributes();
}
