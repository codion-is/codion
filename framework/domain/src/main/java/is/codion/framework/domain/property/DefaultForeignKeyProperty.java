/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultForeignKeyProperty extends DefaultProperty<Entity> implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private final EntityType<Entity> referencedEntityType;
  private final List<Attribute<?>> attributes;
  private final List<ColumnProperty<?>> columnProperties;
  private int fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
  private boolean softReference = false;

  /**
   * @param attribute the attribute, note that this is not a column
   * @param caption the property caption
   * @param referencedEntityType the type of the entity referenced by this foreign key
   * @param columnProperties the underlying column properties comprising this foreign key
   */
  DefaultForeignKeyProperty(final Attribute<Entity> attribute, final String caption,
                            final EntityType<?> referencedEntityType, final List<ColumnProperty<?>> columnProperties) {
    super(attribute, caption);
    requireNonNull(referencedEntityType, "foreignEntityType");
    validateParameters(attribute, referencedEntityType, columnProperties);
    this.referencedEntityType = (EntityType<Entity>) referencedEntityType;
    this.columnProperties = unmodifiableList(columnProperties);
    this.attributes = unmodifiableList(columnProperties.stream().map(Property::getAttribute).collect(toList()));
  }

  @Override
  public boolean isUpdatable() {
    return columnProperties.stream().allMatch(ColumnProperty::isUpdatable);
  }

  @Override
  public EntityType<Entity> getReferencedEntityType() {
    return referencedEntityType;
  }

  @Override
  public List<Attribute<?>> getColumnAttributes() {
    return attributes;
  }

  @Override
  public List<ColumnProperty<?>> getColumnProperties() {
    return columnProperties;
  }

  @Override
  public int getFetchDepth() {
    return fetchDepth;
  }

  @Override
  public boolean isSoftReference() {
    return softReference;
  }

  /**
   * @return a builder for this property instance
   */
  ForeignKeyProperty.Builder builder(final List<ColumnProperty.Builder<?>> columnPropertyBuilders) {
    requireNonNull(columnPropertyBuilders, "columnPropertyBuilders");
    columnPropertyBuilders.forEach(propertyBuilder -> propertyBuilder.setForeignKeyColumn(true));

    return new DefaultForeignKeyPropertyBuilder(this, columnPropertyBuilders);
  }

  private static void validateParameters(final Attribute<Entity> attribute, final EntityType<?> foreignEntityType,
                                         final List<ColumnProperty<?>> columnProperties) {
    if (nullOrEmpty(columnProperties)) {
      throw new IllegalArgumentException("No column properties specified");
    }
    for (final ColumnProperty<?> columnProperty : columnProperties) {
      requireNonNull(columnProperty, "columnProperty");
      if (columnProperty.getAttribute().equals(attribute)) {
        throw new IllegalArgumentException(foreignEntityType + ", column attribute is the same as foreign key attribute: " + attribute);
      }
    }
  }

  private static final class DefaultForeignKeyPropertyBuilder
          extends DefaultPropertyBuilder<Entity> implements ForeignKeyProperty.Builder {

    private final DefaultForeignKeyProperty foreignKeyProperty;
    private final List<ColumnProperty.Builder<?>> columnPropertyBuilders;

    private DefaultForeignKeyPropertyBuilder(final DefaultForeignKeyProperty foreignKeyProperty,
                                             final List<ColumnProperty.Builder<?>> columnPropertyBuilders) {
      super(foreignKeyProperty);
      this.foreignKeyProperty = foreignKeyProperty;
      this.columnPropertyBuilders = columnPropertyBuilders;
    }

    @Override
    public ForeignKeyProperty get() {
      return foreignKeyProperty;
    }

    @Override
    public ForeignKeyProperty.Builder insertable(final boolean insertable) {
      columnPropertyBuilders.forEach(builder -> builder.insertable(insertable));
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder updatable(final boolean updatable) {
      columnPropertyBuilders.forEach(builder -> builder.updatable(updatable));
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder readOnly(final boolean readOnly) {
      insertable(!readOnly);
      updatable(!readOnly);
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder nullable(final boolean nullable) {
      super.nullable(nullable);
      columnPropertyBuilders.forEach(builder -> builder.nullable(nullable));
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder fetchDepth(final int fetchDepth) {
      foreignKeyProperty.fetchDepth = fetchDepth;
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder softReference(final boolean softReference) {
      foreignKeyProperty.softReference = softReference;
      return this;
    }
  }
}
