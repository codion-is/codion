/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PropertySearchModel extends AbstractSearchModel {

  private final EntityComboBoxModel entityComboBoxModel;
  private final EntityLookupModel entityLookupModel;

  private boolean updatingModel = false;

  /**
   * Constructs a PropertySearchModel instance
   * @param property the property
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public PropertySearchModel(final Property property) {
    super(property);
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
    super(property);
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
    super(property);
    this.entityComboBoxModel = entityComboBoxModel;
    this.entityLookupModel = null;
    bindComboBoxEvents();
  }

  /** {@inheritDoc} */
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
    final StringBuilder stringBuilder = new StringBuilder(getProperty().getPropertyID());
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
    if (entityComboBoxModel != null && !entityComboBoxModel.isDataInitialized()) {
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

  public PropertyCriteria getPropertyCriteria() {
    final PropertyCriteria criteria = getValueCount(getSearchType()) == 1 ?
            new PropertyCriteria(getProperty(), getSearchType(), getUpperBound()) :
            new PropertyCriteria(getProperty(), getSearchType(), getLowerBound(), getUpperBound());

    criteria.setCaseSensitive(isCaseSensitive());

    return criteria;
  }

  private String toString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection)
      for (final Object obj : ((Collection) object))
        stringBuilder.append(toString(obj));
    else if (object instanceof Entity)
      stringBuilder.append(((Entity)object).getPrimaryKey().toString());
    else
      stringBuilder.append(object);

    return stringBuilder.toString();
  }

  private void bindLookupModelEvents() {
    entityLookupModel.eventSelectedEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        try {
          updatingModel = true;
          setUpperBound(new ArrayList<Entity>(entityLookupModel.getSelectedEntities()));
        }
        finally {
          updatingModel = false;
        }
      }
    });
    eventUpperBoundChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        if (!updatingModel)//noinspection unchecked
          entityLookupModel.setSelectedEntities((List<Entity>) getUpperBound());
      }
    });
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        if (!updatingModel)
          setUpperBound(entityComboBoxModel.getSelectedEntity());
      }
    });
    eventUpperBoundChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        try {
          updatingModel = true;
          final Object upper = getUpperBound();
          if ((upper instanceof Collection && ((Collection) upper).size() > 0))
            entityComboBoxModel.setSelectedItem(((Collection) upper).iterator().next());
          else
            entityComboBoxModel.setSelectedItem(upper);
        }
        finally {
          updatingModel = false;
        }
      }
    });

    entityComboBoxModel.eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        final Object upper = getUpperBound();
        if ((upper instanceof Collection && ((Collection) upper).size() > 0))
          entityComboBoxModel.setSelectedItem(((Collection) upper).iterator().next());
        else
          entityComboBoxModel.setSelectedItem(upper);
      }
    });
  }
}
