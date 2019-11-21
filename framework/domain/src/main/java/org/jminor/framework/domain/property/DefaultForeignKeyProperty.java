/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.framework.domain.Entity;

import java.sql.Types;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.jminor.common.Util.nullOrEmpty;

final class DefaultForeignKeyProperty extends DefaultProperty implements ForeignKeyProperty {

  private static final long serialVersionUID = 1;

  private final String foreignEntityId;
  private final List<ColumnProperty> columnProperties;
  private final boolean compositeReference;
  private int fetchDepth = Property.FOREIGN_KEY_FETCH_DEPTH.get();
  private boolean softReference = false;

  final transient List<ColumnProperty.Builder> columnPropertyBuilders;

  /**
   * @param propertyId the property ID
   * @param caption the caption
   * @param foreignEntityId the ID of the entity referenced by this foreign key
   * @param columnProperty the underlying column property comprising this foreign key
   */
  DefaultForeignKeyProperty(final String propertyId, final String caption, final String foreignEntityId,
                            final ColumnProperty.Builder columnProperty) {
    this(propertyId, caption, foreignEntityId, singletonList(columnProperty));
  }

  /**
   * @param propertyId the property ID, note that this is not a column name
   * @param caption the property caption
   * @param foreignEntityId the ID of the entity referenced by this foreign key
   * @param columnPropertyBuilders the underlying column properties comprising this foreign key
   * @param foreignProperties the properties referenced, in the same order as the column properties,
   * if null then the primary key properties of the referenced entity are used when required
   */
  DefaultForeignKeyProperty(final String propertyId, final String caption, final String foreignEntityId,
                            final List<ColumnProperty.Builder> columnPropertyBuilders) {
    super(propertyId, Types.OTHER, caption, Entity.class);
    requireNonNull(foreignEntityId, "foreignEntityId");
    validateParameters(propertyId, foreignEntityId, columnPropertyBuilders);
    this.columnPropertyBuilders = columnPropertyBuilders;
    this.columnPropertyBuilders.forEach(propertyBuilder -> propertyBuilder.setForeignKeyProperty(true));
    this.compositeReference = columnPropertyBuilders.size() > 1;
    this.foreignEntityId = foreignEntityId;
    this.columnProperties = unmodifiableList(columnPropertyBuilders.stream()
            .map(ColumnProperty.Builder::get).collect(toList()));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isUpdatable() {
    return columnProperties.stream().allMatch(ColumnProperty::isUpdatable);
  }

  /** {@inheritDoc} */
  @Override
  public String getForeignEntityId() {
    return foreignEntityId;
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getProperties() {
    return columnProperties;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCompositeKey() {
    return compositeReference;
  }

  /** {@inheritDoc} */
  @Override
  public int getFetchDepth() {
    return fetchDepth;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSoftReference() {
    return softReference;
  }

  /** {@inheritDoc} */
  @Override
  public void validateType(final Object value) {
    super.validateType(value);
    if (value != null) {
      final Entity entity = (Entity) value;
      if (!Objects.equals(foreignEntityId, entity.getEntityId())) {
        throw new IllegalArgumentException("Entity of type " + foreignEntityId +
                " expected for property " + this + ", got: " + entity.getEntityId());
      }
    }
  }

  /**
   * @return a builder for this property instance
   */
  ForeignKeyProperty.Builder builder() {
    return new DefaultForeignKeyPropertyBuilder(this);
  }

  private static void validateParameters(final String propertyId, final String foreignEntityId,
                                         final List<ColumnProperty.Builder> columnProperties) {
    if (nullOrEmpty(columnProperties)) {
      throw new IllegalArgumentException("No column properties specified");
    }
    for (final ColumnProperty.Builder columnProperty : columnProperties) {
      requireNonNull(columnProperty, "columnProperty");
      if (columnProperty.get().getPropertyId().equals(propertyId)) {
        throw new IllegalArgumentException(foreignEntityId + ", column propertyId is the same as foreign key propertyId: " + propertyId);
      }
    }
  }

  private static final class DefaultForeignKeyPropertyBuilder
          extends DefaultPropertyBuilder implements ForeignKeyProperty.Builder {

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
    public List<ColumnProperty.Builder> getColmnPropertyBuilders() {
      return foreignKeyProperty.columnPropertyBuilders;
    }

    @Override
    public ForeignKeyProperty.Builder setReadOnly(final boolean readOnly) {
      super.setReadOnly(readOnly);
      foreignKeyProperty.columnPropertyBuilders.forEach(builder -> {
        if (builder.get().isReadOnly() != readOnly) {
          builder.setReadOnly(readOnly);
        }
      });
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder setNullable(final boolean nullable) {
      for (final ColumnProperty.Builder propertyBuilder : foreignKeyProperty.columnPropertyBuilders) {
        propertyBuilder.setNullable(nullable);
      }
      super.setNullable(nullable);
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder setFetchDepth(final int fetchDepth) {
      foreignKeyProperty.fetchDepth = fetchDepth;
      return this;
    }

    @Override
    public ForeignKeyProperty.Builder setSoftReference(final boolean softReference) {
      foreignKeyProperty.softReference = softReference;
      return this;
    }
  }
}
