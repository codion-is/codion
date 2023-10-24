/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
     * @return the column in the parent entity
     */
    Column<T> referencedColumn();
  }

  /**
   * Returns a new {@link Reference} based on the given columns.
   * @param column the local column
   * @param referencedColumn the referenced column
   * @param <T> the column type
   * @return a new {@link Reference} based on the given columns
   */
  static <T> Reference<T> reference(Column<T> column, Column<T> referencedColumn) {
    return new DefaultForeignKey.DefaultReference<>(column, referencedColumn);
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
  }
}
