/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.table.DefaultColumnSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Collections;

/**
 * A default ForeignKeySearchModel implementation.
 */
public class DefaultForeignKeySearchModel extends DefaultColumnSearchModel<Property.ForeignKeyProperty>
        implements ForeignKeySearchModel {

  private final EntityComboBoxModel entityComboBoxModel;
  private final EntityLookupModel entityLookupModel;

  private boolean updatingModel = false;

  /**
   * Constructs a DefaultPropertySearchModel instance
   * @param property the property
   * @param entityLookupModel a EntityLookupModel
   */
  public DefaultForeignKeySearchModel(final Property.ForeignKeyProperty property, final EntityLookupModel entityLookupModel) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.entityLookupModel = entityLookupModel;
    this.entityComboBoxModel = null;
    bindLookupModelEvents();
  }

  /**
   * Constructs a DefaultPropertySearchModel instance
   * @param property the property
   * @param entityComboBoxModel a EntityComboBoxModel
   */
  public DefaultForeignKeySearchModel(final Property.ForeignKeyProperty property, final EntityComboBoxModel entityComboBoxModel) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.entityComboBoxModel = entityComboBoxModel;
    if (entityComboBoxModel != null && entityComboBoxModel.isCleared()) {
      entityComboBoxModel.setSelectedItem(getUpperBound());
    }
    this.entityLookupModel = null;
    bindComboBoxEvents();
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
  public final void refresh() {
    if (entityComboBoxModel != null) {
      entityComboBoxModel.refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    if (entityComboBoxModel != null) {
      entityComboBoxModel.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityComboBoxModel getEntityComboBoxModel() {
    return entityComboBoxModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getEntityLookupModel() {
    return entityLookupModel;
  }

  /** {@inheritDoc} */
  @Override
  public Collection<Entity> getSearchEntities() {
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

  private String toString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : ((Collection) object)) {
        stringBuilder.append(toString(obj));
      }
    }
    else {
      stringBuilder.append(((Entity) object).getPrimaryKey().toString());
    }

    return stringBuilder.toString();
  }

  private void bindLookupModelEvents() {
    entityLookupModel.addSelectedEntitiesListener(new EventListener() {
      @Override
      public void eventOccurred() {
        try {
          updatingModel = true;
          final Collection<Entity> selectedEntities = entityLookupModel.getSelectedEntities();
          setUpperBound(selectedEntities.isEmpty() ? null : selectedEntities);
        }
        finally {
          updatingModel = false;
        }
      }
    });
    addUpperBoundListener(new EventListener() {
      @Override
      public void eventOccurred() {
        if (!updatingModel) {//noinspection unchecked
          final Object upperBound = getUpperBound();
          if (upperBound instanceof Entity) {
            entityLookupModel.setSelectedEntities(Collections.singletonList(((Entity) upperBound)));
          }
          else {//noinspection unchecked
            entityLookupModel.setSelectedEntities((Collection<Entity>) upperBound);
          }
        }
      }
    });
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.addSelectionListener(new EventListener() {
      @Override
      public void eventOccurred() {
        if (!updatingModel) {
          setUpperBound(entityComboBoxModel.getSelectedValue());
        }
      }
    });
    addUpperBoundListener(new EventListener() {
      @Override
      public void eventOccurred() {
        try {
          updatingModel = true;
          final Object upper = getUpperBound();
          if ((upper instanceof Collection && !((Collection) upper).isEmpty())) {
            entityComboBoxModel.setSelectedItem(((Collection) upper).iterator().next());
          }
          else {
            entityComboBoxModel.setSelectedItem(upper);
          }
        }
        finally {
          updatingModel = false;
        }
      }
    });

    entityComboBoxModel.addRefreshListener(new EventListener() {
      @Override
      public void eventOccurred() {
        final Object upper = getUpperBound();
        if ((upper instanceof Collection && !((Collection) upper).isEmpty())) {
          entityComboBoxModel.setSelectedItem(((Collection) upper).iterator().next());
        }
        else {
          entityComboBoxModel.setSelectedItem(upper);
        }
      }
    });
  }
}
