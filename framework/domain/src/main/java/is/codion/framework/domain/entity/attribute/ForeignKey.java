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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.ForeignKeyCondition;

import java.util.List;

/**
 * An {@link Attribute} representing a foreign key relation.
 */
public interface ForeignKey extends Attribute<Entity>, ForeignKeyCondition.Factory {

  /**
   * @return a {@link ForeignKeyDefiner} for this foreign key
   */
  ForeignKeyDefiner define();

  /**
   * @return the entity type referenced by this foreign key
   */
  EntityType referencedType();

  /**
   * @return the {@link Reference}s that comprise this key
   */
  List<Reference<?>> references();

  /**
   * @param column the column
   * @param <T> the column type
   * @return the reference that is based on the given column
   */
  <T> Reference<T> reference(Column<T> column);

  /**
   * Represents a foreign key reference between columns.
   * @param <T> the attribute type
   */
  interface Reference<T> {

    /**
     * @return the column in the child entity
     */
    Column<T> column();

    /**
     * @return the referenced foreign column in the parent entity
     */
    Column<T> foreign();
  }

  /**
   * Returns a new {@link Reference} based on the given columns.
   * @param column the local column
   * @param foreign the referenced foreign column
   * @param <T> the column type
   * @return a new {@link Reference} based on the given columns
   */
  static <T> Reference<T> reference(Column<T> column, Column<T> foreign) {
    return new DefaultForeignKey.DefaultReference<>(column, foreign);
  }

  /**
   * Creates a new {@link ForeignKey} based on the given entityType and references.
   * @param entityType the entityType owning this foreign key
   * @param name the attribute name
   * @param references the references
   * @return a new {@link ForeignKey}
   * @see ForeignKey#reference(Column, Column)
   */
  static ForeignKey foreignKey(EntityType entityType, String name, List<ForeignKey.Reference<?>> references) {
    return new DefaultForeignKey(name, entityType, references);
  }

  /**
   * Provides {@link ForeignKeyDefinition.Builder} instances.
   */
  interface ForeignKeyDefiner extends AttributeDefiner<Entity> {

    /**
     * Instantiates a {@link ForeignKeyDefinition.Builder} instance, using the fetch depth
     * specified by {@link ForeignKeyDefinition#FOREIGN_KEY_FETCH_DEPTH}
     * @return a new {@link ForeignKeyDefinition.Builder}
     * @see ForeignKeyDefinition#FOREIGN_KEY_FETCH_DEPTH
     */
    ForeignKeyDefinition.Builder foreignKey();

    /**
     * Instantiates a {@link ForeignKeyDefinition.Builder} instance.
     * <pre>
     * Fetch depth:
     * -1: the full foreign key graph of the referenced entity is fetched.
     *  0: the referenced entity not fetched.
     *  1: the referenced entity is fetched, without any foreign key references.
     *  2: the referenced entity is fetched, with a single level of foreign key references.
     *  3: the referenced entity is fetched, with two levels of foreign key references.
     *  4: etc...
     *  </pre>
     * @param fetchDepth the number of levels of foreign key references to fetch for this foreign key
     * @return a new {@link ForeignKeyDefinition.Builder}
     * @throws IllegalArgumentException in case fetch depth is less than 0
     */
    ForeignKeyDefinition.Builder foreignKey(int fetchDepth);

    /**
     * Instantiates a {@link ForeignKeyDefinition.Builder} instance, using the fetch depth
     * specified by {@link ForeignKeyDefinition#FOREIGN_KEY_FETCH_DEPTH}.
     * This foreign key is marked as being soft, that is, not based on a physical (table) foreign key
     * and should not prevent deletion
     * @return a new {@link ForeignKeyDefinition.Builder}
     * @see ForeignKeyDefinition#FOREIGN_KEY_FETCH_DEPTH
     */
    ForeignKeyDefinition.Builder softForeignKey();

    /**
     * Instantiates a {@link ForeignKeyDefinition.Builder} instance.
     * This foreign key is marked as being soft, that is, not based on a physical (table) foreign key
     * and should not prevent deletion
     * <pre>
     * Fetch depth:
     * -1: the full foreign key graph of the referenced entity is fetched.
     *  0: the referenced entity not fetched.
     *  1: the referenced entity is fetched, without any foreign key references.
     *  2: the referenced entity is fetched, with a single level of foreign key references.
     *  3: the referenced entity is fetched, with two levels of foreign key references.
     *  4: etc...
     *  </pre>
     * @param fetchDepth the number of levels of foreign key references to fetch for this foreign key
     * @return a new {@link ForeignKeyDefinition.Builder}
     * @throws IllegalArgumentException in case fetch depth is less than 0
     */
    ForeignKeyDefinition.Builder softForeignKey(int fetchDepth);
  }
}
