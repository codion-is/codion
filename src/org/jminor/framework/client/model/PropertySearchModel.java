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

  public long hashCode(final boolean includeSearchState) {
    long ret = getProperty().hashCode();
    if (includeSearchState) {
      ret += getSearchType().hashCode();
      ret += stSearchEnabled.isActive() ? 1 : 0;
      ret += getUpperBound() != null ? getHashCode(getUpperBound()) : 0;
      ret += getLowerBound() != null ? getHashCode(getLowerBound()) : 0;
    }

    return ret;
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
      entityComboBoxModel.setSelectedItem(getUpperBound());
    }
  }

  public PropertyCriteria getPropertyCriteria() {
    if (getValueCount(getSearchType()) == 1)
      return new PropertyCriteria(getProperty(), getSearchType(), getUpperBound());
    else
      return new PropertyCriteria(getProperty(), getSearchType(), getLowerBound(), getUpperBound());
  }

  private long getHashCode(Object obj) {
    long ret = 0;
    if (obj instanceof Collection)
      for (final Object object : ((Collection) obj))
        ret += getHashCode(object);
    else
      ret = obj.hashCode();

    return ret;
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
