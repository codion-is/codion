/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;

import java.util.Map;

/**
 * A class encapsulating a where clause along with columns and their associated values for update.
 * A factory class for {@link Update.Builder} instances via
 * {@link Update#all(EntityType)}, {@link Update#where(Condition)} and
 * {@link Update#builder(Update)}.
 */
public interface Update {

  /**
   * @return the underlying condition instance
   */
  Condition condition();

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
    return new DefaultUpdate.DefaultBuilder(Condition.all(entityType));
  }

  /**
   * @param condition the condition
   * @return a {@link Update.Builder} instance
   */
  static Builder where(Condition condition) {
    return new DefaultUpdate.DefaultBuilder(condition);
  }

  /**
   * @param update the update
   * @return a {@link Update.Builder} instance
   */
  static Builder builder(Update update) {
    return new DefaultUpdate.DefaultBuilder(update);
  }
}
