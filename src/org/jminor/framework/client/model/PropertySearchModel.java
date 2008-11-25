/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.PropertyCriteria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

public class PropertySearchModel extends AbstractSearchModel {

  private final EntityComboBoxModel entityComboBoxModel;

  private boolean updatingModel = false;

  /**
   * Constructs a PropertySearchModel instance
   * @param property the property
   * @param entityComboBoxModel a EntityComboBoxModel
   * @throws IllegalArgumentException if an illegal constant is used
   */
  public PropertySearchModel(final Property property, final EntityComboBoxModel entityComboBoxModel) {
    super(property);
    this.entityComboBoxModel = entityComboBoxModel;
    bindComboBoxEvents();
  }

  /** {@inheritDoc} */
  public boolean include(final Object object) {
    return true;
  }

  /**
   * @param value Value to set for property 'upperBound'.
   */
  public void setUpperBound(final Entity value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value Value to set for property 'lowerBound'.
   */
  public void setLowerBound(final Entity value) {
    setLowerBound((Object) value);
  }

  public String toString() {
    final StringBuffer ret = new StringBuffer(getProperty().propertyID).append(getSearchType());
    if (isSearchEnabled()) {
      ret.append(getUpperBound() != null ? toString(getUpperBound()) : "null");
      ret.append(getLowerBound() != null ? toString(getLowerBound()) : "null");
    }

    return ret.toString();
  }

  /**
   * @return Value for property 'entityComboBoxModel'.
   */
  public EntityComboBoxModel getEntityComboBoxModel() {
    return entityComboBoxModel;
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

  private String toString(final Object obj) {
    final StringBuffer ret = new StringBuffer(getSearchType().toString());
    if (obj instanceof Collection)
      for (final Object object : ((Collection) obj))
        ret.append(toString(object));
    else if (obj instanceof Entity)
      ret.append(((Entity)obj).getPrimaryKey().toString());
    else
      ret.append(obj);

    return ret.toString();
  }

  private void bindComboBoxEvents() {
    if (entityComboBoxModel != null) {
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
    }
    if (entityComboBoxModel != null) {
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
}
