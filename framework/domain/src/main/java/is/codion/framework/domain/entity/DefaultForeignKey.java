/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultForeignKey extends DefaultAttribute<Entity> implements ForeignKey {

  private static final long serialVersionUID = 1;

  private final List<Reference<?>> references;

  DefaultForeignKey(String name, EntityType entityType, List<Reference<?>> references) {
    super(name, Entity.class, entityType);
    this.references = validate(requireNonNull(references));
  }

  @Override
  public EntityType referencedType() {
    return references.get(0).referencedColumn().entityType();
  }

  @Override
  public List<Reference<?>> references() {
    return references;
  }

  @Override
  public <T> Reference<T> reference(Column<T> column) {
    requireNonNull(column);
    for (int i = 0; i < references.size(); i++) {
      Reference<?> reference = references.get(i);
      if (reference.column().equals(column)) {
        return (Reference<T>) reference;
      }
    }

    throw new IllegalArgumentException("Column " + column + " is not part of foreign key " + name());
  }

  private List<Reference<?>> validate(List<Reference<?>> references) {
    if (references.isEmpty()) {
      throw new IllegalArgumentException("No references provided for foreign key: " + name());
    }
    EntityType referencedEntityType = references.get(0).referencedColumn().entityType();
    List<Reference<?>> referenceList = new ArrayList<>(references.size());
    for (Reference<?> reference : references) {
      if (!entityType().equals(reference.column().entityType())) {
        throw new IllegalArgumentException("Entity type " + entityType() +
                " expected, got " + reference.column().entityType());
      }
      if (!referencedEntityType.equals(reference.referencedColumn().entityType())) {
        throw new IllegalArgumentException("Entity type " + referencedEntityType +
                " expected, got " + reference.referencedColumn().entityType());
      }
      if (referenceList.stream().anyMatch(existingReference -> existingReference.column().equals(reference.column()))) {
        throw new IllegalArgumentException("Foreign key already contains a reference for column: " + reference.column());
      }

      referenceList.add(reference);
    }

    return Collections.unmodifiableList(referenceList);
  }

  static final class DefaultReference<T> implements Reference<T>, Serializable {

    private static final long serialVersionUID = 1;

    private final Column<T> column;
    private final Column<T> referencedColumn;

    DefaultReference(Column<T> column, Column<T> referencedColumn) {
      if (requireNonNull(column, "column").equals(requireNonNull(referencedColumn, "referencedColumn"))) {
        throw new IllegalArgumentException("column and referencedColumn can not be the same");
      }
      this.column = column;
      this.referencedColumn = referencedColumn;
    }

    @Override
    public Column<T> column() {
      return column;
    }

    @Override
    public Column<T> referencedColumn() {
      return referencedColumn;
    }
  }
}
