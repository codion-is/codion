/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.AbstractSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: Björn Darri<br>
 * Date: 19.7.2010<br>
 * Time: 13:53:07
 */
public class DefaultForeignKeySearchModel extends AbstractSearchModel<Property.ForeignKeyProperty>
        implements ForeignKeySearchModel {

  private final EntityComboBoxModel entityComboBoxModel;
  private final EntityLookupModel entityLookupModel;

  private boolean updatingModel = false;

  /**
   * Constructs a DefaultPropertySearchModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public DefaultForeignKeySearchModel(final Property.ForeignKeyProperty property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.entityLookupModel = null;
    this.entityComboBoxModel = null;
  }

  /**
   * Constructs a DefaultPropertySearchModel instance
   * @param property the property
   * @param entityLookupModel a EntityLookupModel
   * @throws IllegalArgumentException if an illegal constant is used
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
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public DefaultForeignKeySearchModel(final Property.ForeignKeyProperty property, final EntityComboBoxModel entityComboBoxModel) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.entityComboBoxModel = entityComboBoxModel;
    this.entityLookupModel = null;
    bindComboBoxEvents();
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getSearchKey().getPropertyID());
    if (isSearchEnabled()) {
      stringBuilder.append(getSearchType());
      stringBuilder.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      stringBuilder.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return stringBuilder.toString();
  }

  public void refresh() {
    if (entityComboBoxModel != null) {
      entityComboBoxModel.refresh();
    }
  }

  public void clear() {
    if (entityComboBoxModel != null) {
      entityComboBoxModel.clear();
    }
  }

  /**
   * @return the EntityComboBoxModel used by this PropertySearchModel, if any
   */
  public EntityComboBoxModel getEntityComboBoxModel() {
    return entityComboBoxModel;
  }

  /**
   * @return the EntityLookupModel used by this PropertySearchModel, if any
   */
  public EntityLookupModel getEntityLookupModel() {
    return entityLookupModel;
  }

  /**
   * Ensures that the data this PropertySearchModel relies on (in ComboBoxModels f.ex) has been initialized
   */
  public void initialize() {
    if (entityComboBoxModel != null && entityComboBoxModel.isCleared()) {
      entityComboBoxModel.refresh();
      try {
        updatingModel = true;//to prevent a round trip to setUpperBound()
        entityComboBoxModel.setSelectedItem(getUpperBound());
      }
      finally {
        updatingModel = false;
      }
    }
  }

  /**
   * @return a Criteria based on the values in this search model.
   */
  public Criteria<Property.ColumnProperty> getCriteria() {
    return EntityCriteriaUtil.foreignKeyCriteria(getSearchKey(), getSearchType(), getUpperBound());
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
    entityLookupModel.eventSelectedEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          updatingModel = true;
          final Collection<Entity> selectedEntities = entityLookupModel.getSelectedEntities();
          setUpperBound(selectedEntities.isEmpty() ? null : new ArrayList<Entity>(selectedEntities));
        }
        finally {
          updatingModel = false;
        }
      }
    });
    eventUpperBoundChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!updatingModel) {//noinspection unchecked
          entityLookupModel.setSelectedEntities((List<Entity>) getUpperBound());
        }
      }
    });
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!updatingModel) {
          setUpperBound(entityComboBoxModel.getSelectedEntity());
        }
      }
    });
    eventUpperBoundChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
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

    entityComboBoxModel.eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
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
