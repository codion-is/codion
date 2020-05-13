/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.condition;

import dev.codion.framework.domain.entity.OrderBy;

import java.util.List;

/**
 * A class encapsulating select query parameters.
 */
public interface EntitySelectCondition extends EntityCondition {

  /**
   * @return the OrderBy for this condition, null if none is specified
   */
  OrderBy getOrderBy();

  /**
   * Sets the OrderBy for this condition
   * @param orderBy the OrderBy to use when applying this condition
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setOrderBy(OrderBy orderBy);

  /**
   * @return the limit to use for the given condition
   */
  int getLimit();

  /**
   * @param limit the limit to use for this condition
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setLimit(int limit);

  /**
   * @return the offset to use for the given condition
   */
  int getOffset();

  /**
   * @param offset the offset to use for this condition
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setOffset(int offset);

  /**
   * @return true if this select should lock the result for update
   */
  boolean isForUpdate();

  /**
   * Marks this condition as a select for update query, this means the resulting records
   * will be locked by the given connection until unlocked by running another (non - select for update)
   * query on the same connection or performing an update
   * @param forUpdate if true then the results should be locked for update
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setForUpdate(boolean forUpdate);

  /**
   * @return the maximum number of records to fetch from the result
   */
  int getFetchCount();

  /**
   * @param fetchCount the maximum number of records to fetch from the result
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setFetchCount(int fetchCount);

  /**
   * Returns the number of levels of foreign key values to fetch, with 0 meaning no referenced entities
   * should be fetched, -1 no limit and null unspecified (use default).
   * @param foreignKeyPropertyId the foreign key property id
   * @return the number of levels of foreign key values to fetch
   */
  Integer getForeignKeyFetchDepth(String foreignKeyPropertyId);

  /**
   * Limit the levels of foreign keys to fetch via the given foreign key property
   * @param foreignKeyPropertyId the property id
   * @param fetchDepth the foreign key fetch depth limit
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setForeignKeyFetchDepth(String foreignKeyPropertyId, int fetchDepth);

  /**
   * Limit the levels of foreign keys to fetch
   * @param fetchDepth the foreign key fetch depth limit
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setForeignKeyFetchDepth(int fetchDepth);

  /**
   * Sets the properties to include in the resulting Entities,
   * including the column properties involved in a foreign key
   * causes the foreign key values to be populated.
   * If you want the primary key to be populated you must include
   * the primary key properties.
   * Note that these must be ColumnProperty ids
   * @param propertyIds the ids of the column properties to include
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setSelectPropertyIds(String... propertyIds);

  /**
   * @return the ids of the properties to include in the query result,
   * an empty list if all should be included
   */
  List<String> getSelectPropertyIds();
}
