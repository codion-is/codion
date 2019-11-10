/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.framework.domain.Entity;

import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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

  transient final List<ColumnPropertyBuilder> columnPropertyBuilders;

  /**
   * @param propertyId the property ID
   * @param caption the caption
   * @param foreignEntityId the ID of the entity referenced by this foreign key
   * @param columnProperty the underlying column property comprising this foreign key
   */
  DefaultForeignKeyProperty(final String propertyId, final String caption, final String foreignEntityId,
                            final ColumnPropertyBuilder columnProperty) {
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
                            final List<ColumnPropertyBuilder> columnPropertyBuilders) {
    super(propertyId, Types.OTHER, caption, Entity.class);
    requireNonNull(foreignEntityId, "foreignEntityId");
    validateParameters(propertyId, foreignEntityId, columnPropertyBuilders);
    this.columnPropertyBuilders = columnPropertyBuilders;
    columnPropertyBuilders.forEach(propertyBuilder -> propertyBuilder.setForeignKeyProperty(this));
    this.foreignEntityId = foreignEntityId;
    this.columnProperties = unmodifiableList(columnPropertyBuilders.stream().map((Function<ColumnPropertyBuilder,
            ColumnProperty>) ColumnPropertyBuilder::get).collect(toList()));
    this.compositeReference = this.columnProperties.size() > 1;
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
  public List<ColumnProperty> getProperties() {
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

  void setNullable(final boolean nullable) {
    for (final ColumnPropertyBuilder propertyBuilder : columnPropertyBuilders) {
      propertyBuilder.setNullable(nullable);
    }

    super.setNullable(nullable);
  }

  void setFetchDepth(final int fetchDepth) {
    this.fetchDepth = fetchDepth;
  }

  void setSoftReference(final boolean softReference) {
    this.softReference = softReference;
  }

  private static void validateParameters(final String propertyId, final String foreignEntityId,
                                         final List<ColumnPropertyBuilder> columnProperties) {
    if (nullOrEmpty(columnProperties)) {
      throw new IllegalArgumentException("No column properties specified");
    }
    for (final ColumnPropertyBuilder columnProperty : columnProperties) {
      requireNonNull(columnProperty, "columnProperty");
      if (columnProperty.get().getPropertyId().equals(propertyId)) {
        throw new IllegalArgumentException(foreignEntityId + ", column propertyId is the same as foreign key propertyId: " + propertyId);
      }
    }
  }
}
