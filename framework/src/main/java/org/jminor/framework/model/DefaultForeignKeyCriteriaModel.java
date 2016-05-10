/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.table.DefaultColumnCriteriaModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Collections;

/**
 * A default ForeignKeyCriteriaModel implementation.
 */
public class DefaultForeignKeyCriteriaModel extends DefaultColumnCriteriaModel<Property.ForeignKeyProperty>
        implements ForeignKeyCriteriaModel {

  private final EntityLookupModel entityLookupModel;

  private boolean updatingModel = false;

  /**
   * Constructs a DefaultForeignKeyCriteriaModel instance
   * @param property the property
   */
  public DefaultForeignKeyCriteriaModel(final Property.ForeignKeyProperty property) {
    this(property, null);
  }

  /**
   * Constructs a DefaultForeignKeyCriteriaModel instance
   * @param property the property
   * @param entityLookupModel a EntityLookupModel
   */
  public DefaultForeignKeyCriteriaModel(final Property.ForeignKeyProperty property, final EntityLookupModel entityLookupModel) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
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
      stringBuilder.append(getSearchType());
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
  public Collection<Entity> getCriteriaEntities() {
    final Object upperBound = getUpperBound();
    if (upperBound instanceof Entity) {
      return Collections.singletonList((Entity) upperBound);
    }
    //noinspection unchecked
    return upperBound == null ? Collections.<Entity>emptyList() : (Collection<Entity>) upperBound;
  }

  /** {@inheritDoc} */
  @Override
  public final Criteria<Property.ColumnProperty> getCriteria() {
    final Object upperBound = getUpperBound();
    if (upperBound instanceof Collection) {
      return EntityCriteriaUtil.foreignKeyCriteria(getColumnIdentifier(), getSearchType(), (Collection) upperBound);
    }

    return EntityCriteriaUtil.foreignKeyCriteria(getColumnIdentifier(), getSearchType(), Collections.singletonList(upperBound));
  }

  /** {@inheritDoc} */
  @Override
  public void refresh() {}

  /** {@inheritDoc} */
  @Override
  public void clear() {}

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
    entityLookupModel.addSelectedEntitiesListener(new EventInfoListener<Collection<Entity>>() {
      @Override
      public void eventOccurred(final Collection<Entity> selectedEntities) {
        try {
          setUpdatingModel(true);
          setUpperBound(selectedEntities.isEmpty() ? null : selectedEntities);
        }
        finally {
          setUpdatingModel(false);
        }
      }
    });
    addUpperBoundListener(new EventListener() {
      @Override
      public void eventOccurred() {
        if (!isUpdatingModel()) {//noinspection unchecked
          final Object upperBound = getUpperBound();
          if (upperBound instanceof Entity) {
            entityLookupModel.setSelectedEntities(Collections.singletonList((Entity) upperBound));
          }
          else {//noinspection unchecked
            entityLookupModel.setSelectedEntities((Collection<Entity>) upperBound);
          }
        }
      }
    });
  }
}
