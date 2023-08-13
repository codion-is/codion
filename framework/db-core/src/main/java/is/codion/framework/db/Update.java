/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;

import java.util.Map;

/**
 * A class encapsulating a where clause along with columns and their associated values for update.
 * A factory class for {@link Update.Builder} instances via
 * {@link Update#all(EntityType)}, {@link Update#where(Criteria)} and
 * {@link Update#builder(Update)}.
 */
public interface Update {

  /**
   * @return the underlying Criteria instance
   */
  Criteria criteria();

  /**
   * @return an unmodifiable view of the new values mapped to their respective columns
   */
  Map<Column<?>, Object> columnValues();

  /**
   * Builds an {@link Update}.
   */
  interface Builder {

    /**
     * Adds a column value to update
     * @param column the column
     * @param value the new value
     * @param <T> the value type
     * @return this builder
     */
    <T> Builder set(Column<?> column, T value);

    /**
     * @return a new {@link Update} instance based on this builder
     */
    Update build();
  }

  /**
   * @param entityType the entity type
   * @return a {@link Update.Builder} instance
   */
  static Builder all(EntityType entityType) {
    return new DefaultUpdate.DefaultBuilder(Criteria.all(entityType));
  }

  /**
   * @param criteria the criteria
   * @return a {@link Update.Builder} instance
   */
  static Builder where(Criteria criteria) {
    return new DefaultUpdate.DefaultBuilder(criteria);
  }

  /**
   * @param update the update
   * @return a {@link Update.Builder} instance
   */
  static Builder builder(Update update) {
    return new DefaultUpdate.DefaultBuilder(update);
  }
}
