/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultForeignKeyCriteriaModel;
import org.jminor.framework.model.EntityComboBoxModel;

import java.util.Collection;

/**
 * A {@link org.jminor.framework.model.ForeignKeyCriteriaModel} based around a {@link EntityComboBoxModel}
 */
public final class SwingForeignKeyCriteriaModel extends DefaultForeignKeyCriteriaModel {

  private final EntityComboBoxModel entityComboBoxModel;

  /**
   * Constructs a SwingForeignKeyCriteriaModel instance
   * @param property the property
   * @param entityComboBoxModel a EntityComboBoxModel
   */
  public SwingForeignKeyCriteriaModel(final Property.ForeignKeyProperty property, final EntityComboBoxModel entityComboBoxModel) {
    super(property);
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
    entityComboBoxModel.addSelectionListener(new EventInfoListener<Entity>() {
      @Override
      public void eventOccurred(final Entity selected) {
        if (!isUpdatingModel()) {
          setUpperBound(selected);
        }
      }
    });
    addUpperBoundListener(new EventListener() {
      @Override
      public void eventOccurred() {
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
      }
    });

    entityComboBoxModel.addRefreshListener(new EventListener() {
      @Override
      public void eventOccurred() {
        final Object upper = getUpperBound();
        if (upper instanceof Collection && !((Collection) upper).isEmpty()) {
          entityComboBoxModel.setSelectedItem((Entity) ((Collection) upper).iterator().next());
        }
        else {
          entityComboBoxModel.setSelectedItem((Entity) upper);
        }
      }
    });
  }
}
