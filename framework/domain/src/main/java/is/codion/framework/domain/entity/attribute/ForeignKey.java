/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

/**
 * An {@link Attribute} representing a foreign key relation.
 */
public interface ForeignKey extends Attribute<Entity> {

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
   * Instantiates a {@link ForeignKeyDefinition.Builder} instance.
   * @return a new {@link ForeignKeyDefinition.Builder}
   */
  ForeignKeyDefinition.Builder foreignKey();

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
}
