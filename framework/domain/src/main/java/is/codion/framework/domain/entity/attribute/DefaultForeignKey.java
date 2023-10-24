/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultAttribute.DefaultAttributeDefiner;
import is.codion.framework.domain.entity.condition.Condition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static is.codion.framework.domain.entity.condition.ForeignKeyCondition.factory;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKey implements ForeignKey, Serializable {

  private static final long serialVersionUID = 1;

  private final Attribute<Entity> attribute;
  private final List<Reference<?>> references;

  DefaultForeignKey(String name, EntityType entityType, List<Reference<?>> references) {
    this.attribute = new DefaultAttribute<>(name, Entity.class, entityType);
    this.references = validate(requireNonNull(references));
  }

  @Override
  public Type<Entity> type() {
    return attribute.type();
  }

  @Override
  public String name() {
    return attribute.name();
  }

  @Override
  public EntityType entityType() {
    return attribute.entityType();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultForeignKey)) {
      return false;
    }
    DefaultForeignKey that = (DefaultForeignKey) object;

    return attribute.equals(that.attribute);
  }

  @Override
  public int hashCode() {
    return attribute.hashCode();
  }

  @Override
  public String toString() {
    return attribute.toString();
  }

  @Override
  public ForeignKeyDefiner define() {
    return new DefaultForeignKeyDefiner(this);
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

  @Override
  public Condition equalTo(Entity value) {
    return factory(this).equalTo(value);
  }

  @Override
  public Condition notEqualTo(Entity value) {
    return factory(this).notEqualTo(value);
  }

  @Override
  public Condition in(Entity... values) {
    return factory(this).in(values);
  }

  @Override
  public Condition notIn(Entity... values) {
    return factory(this).notIn(values);
  }

  @Override
  public Condition in(Collection<? extends Entity> values) {
    return factory(this).in(values);
  }

  @Override
  public Condition notIn(Collection<? extends Entity> values) {
    return factory(this).notIn(values);
  }

  @Override
  public Condition isNull() {
    return factory(this).isNull();
  }

  @Override
  public Condition isNotNull() {
    return factory(this).isNotNull();
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

  private static final class DefaultForeignKeyDefiner extends DefaultAttributeDefiner<Entity> implements ForeignKeyDefiner {

    private final ForeignKey foreignKey;

    private DefaultForeignKeyDefiner(ForeignKey foreignKey) {
      super(foreignKey);
      this.foreignKey = foreignKey;
    }

    @Override
    public ForeignKeyDefinition.Builder foreignKey() {
      return foreignKey(ForeignKeyDefinition.FOREIGN_KEY_FETCH_DEPTH.get());
    }

    @Override
    public ForeignKeyDefinition.Builder foreignKey(int fetchDepth) {
      if (fetchDepth < -1) {
        throw new IllegalArgumentException("Fetch depth must be at least -1: " + foreignKey);
      }

      return new DefaultForeignKeyDefinition.DefaultForeignKeyDefinitionBuilder(foreignKey, fetchDepth);
    }
  }
}
