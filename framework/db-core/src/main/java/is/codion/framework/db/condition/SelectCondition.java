/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;

import java.util.Collection;
import java.util.Optional;

/**
 * A class encapsulating select query parameters.
 * A factory class for {@link SelectCondition.Builder} instances via
 * {@link SelectCondition#all(EntityType)}, {@link SelectCondition#where(Criteria)} and
 * {@link SelectCondition#builder(Condition)}.
 */
public interface SelectCondition extends Condition {

  int DEFAULT_QUERY_TIMEOUT_SECONDS = 120;

  /**
   * @return the OrderBy for this condition, an empty Optional if none is specified
   */
  Optional<OrderBy> orderBy();

  /**
   * @return the limit to use for the given condition, -1 for no limit
   */
  int limit();

  /**
   * @return the offset to use for the given condition, -1 for no offset
   */
  int offset();

  /**
   * @return true if this select should lock the result for update
   */
  boolean forUpdate();

  /**
   * @return the query timeout
   */
  int queryTimeout();

  /**
   * @return the global fetch depth limit for this condition, an empty Optional if none has been specified
   */
  Optional<Integer> fetchDepth();

  /**
   * Returns the number of levels of foreign key values to fetch, with 0 meaning no referenced entities
   * should be fetched, -1 no limit and an empty Optional if unspecified (use default).
   * @param foreignKey the foreign key
   * @return the number of levels of foreign key values to fetch
   */
  Optional<Integer> fetchDepth(ForeignKey foreignKey);

  /**
   * @return the attributes to include in the query result,
   * an empty Collection if all should be included
   */
  Collection<Attribute<?>> attributes();

  /**
   * Builds a {@link SelectCondition}.
   */
  interface Builder {

    /**
     * Sets the OrderBy for this condition
     * @param orderBy the OrderBy to use when applying this condition
     * @return this builder instance
     */
    Builder orderBy(OrderBy orderBy);

    /**
     * @param limit the limit to use for this condition
     * @return this builder instance
     */
    Builder limit(int limit);

    /**
     * @param offset the offset to use for this condition
     * @return this builder instance
     */
    Builder offset(int offset);

    /**
     * Marks this condition as a select for update query, this means the resulting records
     * will be locked by the given connection until unlocked by running another (non - select for update)
     * query on the same connection or performing an update
     * @return this builder instance
     */
    Builder forUpdate();

    /**
     * Limit the levels of foreign keys to fetch
     * @param fetchDepth the foreign key fetch depth limit
     * @return this builder instance
     */
    Builder fetchDepth(int fetchDepth);

    /**
     * Limit the levels of foreign keys to fetch via the given foreign key
     * @param foreignKey the foreign key
     * @param fetchDepth the foreign key fetch depth limit
     * @return this builder instance
     */
    Builder fetchDepth(ForeignKey foreignKey, int fetchDepth);

    /**
     * Sets the attributes to include in the query result. An empty array means all attributes should be included.
     * Note that primary key attribute values are always included.
     * @param attributes the attributes to include
     * @param <T> the attribute type
     * @return this builder instance
     */
    <T extends Attribute<?>> Builder attributes(T... attributes);

    /**
     * Sets the attributes to include in the query result. An empty Collection means all attributes should be included.
     * Note that primary key attribute values are always included.
     * @param attributes the attributes to include
     * @return this builder instance
     */
    Builder attributes(Collection<? extends Attribute<?>> attributes);

    /**
     * @param queryTimeout the query timeout, 0 for no timeout
     * @return this builder instance
     */
    Builder queryTimeout(int queryTimeout);

    /**
     * @return a new {@link SelectCondition} instance based on this builder
     */
    SelectCondition build();
  }

  /**
   * @param entityType the entity type
   * @return a new {@link SelectCondition.Builder} instance
   */
  static Builder all(EntityType entityType) {
    return new DefaultSelectCondition.DefaultBuilder(Criteria.all(entityType));
  }

  /**
   * @param criteria the criteria
   * @return a new {@link SelectCondition.Builder} instance
   */
  static Builder where(Criteria criteria) {
    return new DefaultSelectCondition.DefaultBuilder(criteria);
  }

  /**
   * @param condition the condition
   * @return a new {@link SelectCondition.Builder} instance
   */
  static Builder builder(Condition condition) {
    return new DefaultSelectCondition.DefaultBuilder(condition);
  }
}
