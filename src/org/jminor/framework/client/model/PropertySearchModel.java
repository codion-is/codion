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
 * A class for searching a set of entities based on a property.
 */
public class PropertySearchModel extends AbstractSearchModel<Property> {

  private final EntityComboBoxModel entityComboBoxModel;
  private final EntityLookupModel entityLookupModel;

  private boolean updatingModel = false;

  /**
   * Constructs a PropertySearchModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public PropertySearchModel(final Property property) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.entityLookupModel = null;
    this.entityComboBoxModel = null;
  }

  /**
   * Constructs a PropertySearchModel instance
   * @param property the property
   * @param entityLookupModel a EntityLookupModel
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public PropertySearchModel(final Property property, final EntityLookupModel entityLookupModel) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.entityLookupModel = entityLookupModel;
    this.entityComboBoxModel = null;
    bindLookupModelEvents();
  }

  /**
   * Constructs a PropertySearchModel instance
   * @param property the property
   * @param entityComboBoxModel a EntityComboBoxModel
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public PropertySearchModel(final Property property, final EntityComboBoxModel entityComboBoxModel) {
    super(property, property.getType(), (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER));
    this.entityComboBoxModel = entityComboBoxModel;
    this.entityLookupModel = null;
    bindComboBoxEvents();
  }

  @Override
  public boolean include(final Object object) {
    return true;
  }

  /**
   * @param value the upper bound
   */
  public void setUpperBound(final Entity value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the lower bound
   */
  public void setLowerBound(final Entity value) {
    setLowerBound((Object) value);
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder(getSearchProperty().getPropertyID());
    if (isSearchEnabled()) {
      stringBuilder.append(getSearchType());
      stringBuilder.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      stringBuilder.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return stringBuilder.toString();
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
    if (entityComboBoxModel != null && entityComboBoxModel.isClear()) {
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
  public Criteria<Property> getPropertyCriteria() {
    return getValueCount(getSearchType()) == 1 ?
            EntityCriteriaUtil.propertyCriteria(getSearchProperty(), isCaseSensitive(), getSearchType(), getUpperBound()) :
            EntityCriteriaUtil.propertyCriteria(getSearchProperty(), isCaseSensitive(), getSearchType(), getLowerBound(), getUpperBound());
  }

  private String toString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : ((Collection) object)) {
        stringBuilder.append(toString(obj));
      }
    }
    else if (object instanceof Entity) {
      stringBuilder.append(((Entity) object).getPrimaryKey().toString());
    }
    else {
      stringBuilder.append(object);
    }

    return stringBuilder.toString();
  }

  private void bindLookupModelEvents() {
    entityLookupModel.eventSelectedEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          updatingModel = true;
          final Collection<Entity> selectedEntities = entityLookupModel.getSelectedEntities();
          setUpperBound(selectedEntities.size() == 0 ? null : new ArrayList<Entity>(selectedEntities));
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
          if ((upper instanceof Collection && ((Collection) upper).size() > 0)) {
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
        if ((upper instanceof Collection && ((Collection) upper).size() > 0)) {
          entityComboBoxModel.setSelectedItem(((Collection) upper).iterator().next());
        }
        else {
          entityComboBoxModel.setSelectedItem(upper);
        }
      }
    });
  }
}
