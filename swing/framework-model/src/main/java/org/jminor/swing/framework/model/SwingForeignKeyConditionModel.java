/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultForeignKeyConditionModel;
import org.jminor.framework.model.EntityComboBoxModel;

import java.util.Collection;

/**
 * A {@link org.jminor.framework.model.ForeignKeyConditionModel} based around a {@link EntityComboBoxModel}
 */
public final class SwingForeignKeyConditionModel extends DefaultForeignKeyConditionModel {

  private final EntityComboBoxModel entityComboBoxModel;

  /**
   * Constructs a SwingForeignKeyConditionModel instance
   * @param property the property
   * @param entityComboBoxModel a EntityComboBoxModel
   */
  public SwingForeignKeyConditionModel(final EntityConditions entityConditions, final Property.ForeignKeyProperty property,
                                       final EntityComboBoxModel entityComboBoxModel) {
    super(entityConditions, property);
    this.entityComboBoxModel = entityComboBoxModel;
    if (entityComboBoxModel != null && entityComboBoxModel.isCleared()) {
      entityComboBoxModel.setSelectedItem((Entity) getUpperBound());
    }
    bindComboBoxEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    entityComboBoxModel.refresh();
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    entityComboBoxModel.clear();
  }

  public final EntityComboBoxModel getEntityComboBoxModel() {
    return entityComboBoxModel;
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.addSelectionListener(selected -> {
      if (!isUpdatingModel()) {
        setUpperBound(selected);
      }
    });
    addUpperBoundListener(() -> {
      try {
        setUpdatingModel(true);
        final Object upper = getUpperBound();
        if (upper instanceof Collection && !((Collection) upper).isEmpty()) {
          entityComboBoxModel.setSelectedItem((Entity) ((Collection) upper).iterator().next());
        }
        else {
          entityComboBoxModel.setSelectedItem((Entity) upper);
        }
      }
      finally {
        setUpdatingModel(false);
      }
    });

    entityComboBoxModel.addRefreshListener(() -> {
      final Object upper = getUpperBound();
      if (upper instanceof Collection && !((Collection) upper).isEmpty()) {
        entityComboBoxModel.setSelectedItem((Entity) ((Collection) upper).iterator().next());
      }
      else {
        entityComboBoxModel.setSelectedItem((Entity) upper);
      }
    });
  }
}
