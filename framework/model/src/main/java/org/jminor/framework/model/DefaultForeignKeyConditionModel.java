/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Collections;

/**
 * A default ForeignKeyConditionModel implementation.
 */
public class DefaultForeignKeyConditionModel extends DefaultColumnConditionModel<Property.ForeignKeyProperty>
        implements ForeignKeyConditionModel {

  private final EntityLookupModel entityLookupModel;

  private boolean updatingModel = false;

  /**
   * Constructs a DefaultForeignKeyConditionModel instance
   * @param property the property
   */
  public DefaultForeignKeyConditionModel(final Property.ForeignKeyProperty property) {
    this(property, null);
  }

  /**
   * Constructs a DefaultForeignKeyConditionModel instance
   * @param property the property
   * @param entityLookupModel a EntityLookupModel
   */
  public DefaultForeignKeyConditionModel(final Property.ForeignKeyProperty property, final EntityLookupModel entityLookupModel) {
    super(property, property.getType(), Property.WILDCARD_CHARACTER.get());
    this.entityLookupModel = entityLookupModel;
    if (entityLookupModel != null) {
      bindLookupModelEvents();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getColumnIdentifier().getPropertyID());
    if (isEnabled()) {
      stringBuilder.append(getConditionType());
      stringBuilder.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      stringBuilder.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return stringBuilder.toString();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getEntityLookupModel() {
    return entityLookupModel;
  }

  /** {@inheritDoc} */
  @Override
  public Collection<Entity> getConditionEntities() {
    final Object upperBound = getUpperBound();
    if (upperBound instanceof Entity) {
      return Collections.singletonList((Entity) upperBound);
    }
    //noinspection unchecked
    return upperBound == null ? Collections.<Entity>emptyList() : (Collection<Entity>) upperBound;
  }

  /** {@inheritDoc} */
  @Override
  public final Condition<Property.ColumnProperty> getCondition() {
    final Object upperBound = getUpperBound();
    if (upperBound instanceof Collection) {
      return EntityConditions.foreignKeyCondition(getColumnIdentifier(), getConditionType(), (Collection) upperBound);
    }

    return EntityConditions.foreignKeyCondition(getColumnIdentifier(), getConditionType(), Collections.singletonList(upperBound));
  }

  /** {@inheritDoc} */
  @Override
  public void refresh() {/*Nothing to refresh in this default implementation*/}

  /** {@inheritDoc} */
  @Override
  public void clear() {/*Nothing to clear in this default implementation*/}

  protected boolean isUpdatingModel() {
    return updatingModel;
  }

  protected void setUpdatingModel(final boolean updatingModel) {
    this.updatingModel = updatingModel;
  }

  private String toString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : (Collection) object) {
        stringBuilder.append(toString(obj));
      }
    }
    else {
      stringBuilder.append(((Entity) object).getKey().toString());
    }

    return stringBuilder.toString();
  }

  private void bindLookupModelEvents() {
    entityLookupModel.addSelectedEntitiesListener(selectedEntities -> {
      try {
        setUpdatingModel(true);
        setUpperBound(selectedEntities.isEmpty() ? null : selectedEntities);
      }
      finally {
        setUpdatingModel(false);
      }
    });
    addUpperBoundListener(() -> {
      if (!isUpdatingModel()) {//noinspection unchecked
        final Object upperBound = getUpperBound();
        if (upperBound instanceof Entity) {
          entityLookupModel.setSelectedEntities(Collections.singletonList((Entity) upperBound));
        }
        else {//noinspection unchecked
          entityLookupModel.setSelectedEntities((Collection<Entity>) upperBound);
        }
      }
    });
  }
}
