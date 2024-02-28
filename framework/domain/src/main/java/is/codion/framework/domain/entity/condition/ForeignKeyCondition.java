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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;

/**
 * A ForeignKey based condition.
 */
public interface ForeignKeyCondition extends Condition {

  /**
   * Creates {@link ForeignKeyCondition}s.
   */
  interface Factory {

    /**
     * Returns a 'equalTo' {@link Condition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link Condition}
     */
    Condition equalTo(Entity value);

    /**
     * Returns a 'notEqualTo' {@link Condition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link Condition}
     */
    Condition notEqualTo(Entity value);

    /**
     * Returns a 'in' {@link Condition}.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws NullPointerException in case {@code values} is null
     */
    Condition in(Entity... values);

    /**
     * Returns a 'notIn' {@link Condition}.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws NullPointerException in case {@code values} is null
     */
    Condition notIn(Entity... values);

    /**
     * Returns a 'in' {@link Condition}.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws NullPointerException in case {@code values} is null
     */
    Condition in(Collection<Entity> values);

    /**
     * Returns a 'notIn' condition.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws IllegalArgumentException in case {@code values} is null
     */
    Condition notIn(Collection<Entity> values);

    /**
     * Returns a 'isNull' {@link Condition}.
     * @return a {@link Condition}
     */
    Condition isNull();

    /**
     * Returns a 'isNotNull' {@link Condition}.
     * @return a {@link Condition}
     */
    Condition isNotNull();
  }

  /**
   * Instantiates a new {@link Factory} instance
   * @param foreignKey the foreign key
   * @return a new {@link Factory} instance
   */
  static Factory factory(ForeignKey foreignKey) {
    return new DefaultForeignKeyConditionFactory(foreignKey);
  }
}
