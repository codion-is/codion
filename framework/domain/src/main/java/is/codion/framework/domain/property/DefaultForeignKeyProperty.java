/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Entity;

import java.util.List;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultForeignKeyProperty extends DefaultProperty<Entity> implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private final String foreignEntityId;
  private final List<ColumnProperty<?>> columnProperties;
  private final boolean compositeReference;
  private int fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
  private boolean softReference = false;

  private final transient List<ColumnProperty.Builder<?>> columnPropertyBuilders;

  /**
   * @param attribute the attribute
   * @param caption the caption
   * @param foreignEntityId the id of the entity referenced by this foreign key
   * @param columnProperty the underlying column property comprising this foreign key
   */
  DefaultForeignKeyProperty(final Attribute<Entity> attribute, final String caption,
                            final String foreignEntityId, final ColumnProperty.Builder<?> columnProperty) {
    this(attribute, caption, foreignEntityId, singletonList(columnProperty));
  }

  /**
   * @param attribute the attribute, note that this is not a column
   * @param caption the property caption
   * @param foreignEntityId the id of the entity referenced by this foreign key
   * @param columnPropertyBuilders the underlying column properties comprising this foreign key
   */
  DefaultForeignKeyProperty(final Attribute<Entity> attribute, final String caption,
                            final String foreignEntityId, final List<ColumnProperty.Builder<?>> columnPropertyBuilders) {
    super(attribute, caption);
    requireNonNull(foreignEntityId, "foreignEntityId");
    validateParameters(attribute, foreignEntityId, columnPropertyBuilders);
    this.columnPropertyBuilders = columnPropertyBuilders;
    this.columnPropertyBuilders.forEach(propertyBuilder -> propertyBuilder.setForeignKeyProperty(true));
    this.compositeReference = columnPropertyBuilders.size() > 1;
    this.foreignEntityId = foreignEntityId;
    this.columnProperties = unmodifiableList(columnPropertyBuilders.stream()
            .map(ColumnProperty.Builder::get).collect(toList()));
  }

  @Override
  public boolean isInsertable() {
    return columnProperties.stream().allMatch(ColumnProperty::isInsertable);
  }

  @Override
  public boolean isUpdatable() {
    return columnProperties.stream().allMatch(ColumnProperty::isUpdatable);
  }

  @Override
  public String getForeignEntityId() {
    return foreignEntityId;
  }

  @Override
  public List<ColumnProperty<?>> getColumnProperties() {
    return columnProperties;
  }

  @Override
  public boolean isCompositeKey() {
    return compositeReference;
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
  ForeignKeyProperty.Builder builder() {
    return new DefaultForeignKeyPropertyBuilder(this);
  }

  private static void validateParameters(final Attribute<Entity> attribute, final String foreignEntityId,
                                         final List<ColumnProperty.Builder<?>> columnProperties) {
    if (nullOrEmpty(columnProperties)) {
      throw new IllegalArgumentException("No column properties specified");
    }
    for (final ColumnProperty.Builder<?> columnProperty : columnProperties) {
      requireNonNull(columnProperty, "columnProperty");
      if (columnProperty.get().getAttribute().equals(attribute)) {
        throw new IllegalArgumentException(foreignEntityId + ", column attribute is the same as foreign key attribute: " + attribute);
      }
    }
  }

  private static final class DefaultForeignKeyPropertyBuilder
          extends DefaultPropertyBuilder<Entity> implements ForeignKeyProperty.Builder {

    private final DefaultForeignKeyProperty foreignKeyProperty;

    private DefaultForeignKeyPropertyBuilder(final DefaultForeignKeyProperty foreignKeyProperty) {
      super(foreignKeyProperty);
      this.foreignKeyProperty = foreignKeyProperty;
    }

    @Override
    public ForeignKeyProperty get() {
      return foreignKeyProperty;
    }

    @Override
    public List<ColumnProperty.Builder<?>> getColumnPropertyBuilders() {
      return foreignKeyProperty.columnPropertyBuilders;
    }

    @Override
    public ForeignKeyProperty.Builder insertable(final boolean insertable) {
      foreignKeyProperty.columnPropertyBuilders.forEach(builder -> builder.insertable(insertable));
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder updatable(final boolean updatable) {
      foreignKeyProperty.columnPropertyBuilders.forEach(builder -> builder.updatable(updatable));
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
      foreignKeyProperty.columnPropertyBuilders.forEach(builder -> builder.nullable(nullable));
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
