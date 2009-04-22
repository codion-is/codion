/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;

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

  public String toString() {
    final StringBuffer ret = new StringBuffer(getProperty().propertyID);
    if (isSearchEnabled()) {
      ret.append(getSearchType());
      ret.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      ret.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return ret.toString();
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
   * @throws org.jminor.common.model.UserException in case of exception
   */
  public void initialize() throws UserException {
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
    final PropertyCriteria ret = getValueCount(getSearchType()) == 1 ?
            new PropertyCriteria(getProperty(), getSearchType(), getUpperBound()) :
            new PropertyCriteria(getProperty(), getSearchType(), getLowerBound(), getUpperBound());

    ret.setCaseSensitive(isCaseSensitive());

    return ret;
  }

  private String toString(final Object object) {
    final StringBuffer ret = new StringBuffer(getSearchType().toString());
    if (object instanceof Collection)
      for (final Object obj : ((Collection) object))
        ret.append(toString(obj));
    else if (object instanceof Entity)
      ret.append(((Entity)object).getPrimaryKey().toString());
    else
      ret.append(object);

    return ret.toString();
  }

  private void bindLookupModelEvents() {
    entityLookupModel.evtSelectedEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          updatingModel = true;
          setUpperBound(new ArrayList<Entity>(entityLookupModel.getSelectedEntities()));
        }
        finally {
          updatingModel = false;
        }
      }
    });
    evtUpperBoundChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!updatingModel)//noinspection unchecked
          entityLookupModel.setSelectedEntities((List<Entity>) getUpperBound());
      }
    });
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.evtSelectionChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!updatingModel)
          setUpperBound(entityComboBoxModel.getSelectedEntity());
      }
    });
    evtUpperBoundChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
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

    entityComboBoxModel.evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        final Object upper = getUpperBound();
        if ((upper instanceof Collection && ((Collection) upper).size() > 0))
          entityComboBoxModel.setSelectedItem(((Collection) upper).iterator().next());
        else
          entityComboBoxModel.setSelectedItem(upper);
      }
    });
  }
}
