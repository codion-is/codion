/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.DefaultForeignKeyConditionModel;

import java.util.Collection;
import java.util.Objects;

/**
 * A {@link is.codion.framework.model.ForeignKeyConditionModel} based around a {@link SwingEntityComboBoxModel}
 */
public final class SwingForeignKeyConditionModel extends DefaultForeignKeyConditionModel {

  private final SwingEntityComboBoxModel entityComboBoxModel;

  /**
   * Constructs a SwingForeignKeyConditionModel instance
   * @param property the property
   * @param comboBoxModel a SwingEntityComboBoxModel
   */
  public SwingForeignKeyConditionModel(final ForeignKeyProperty property,
                                       final SwingEntityComboBoxModel comboBoxModel) {
    super(property);
    this.entityComboBoxModel = Objects.requireNonNull(comboBoxModel, "comboBoxModel");
    if (entityComboBoxModel.isCleared()) {
      entityComboBoxModel.setSelectedItem(getUpperBound());
    }
    bindComboBoxEvents();
  }

  @Override
  public void refresh() {
    entityComboBoxModel.refresh();
  }

  @Override
  public void clear() {
    entityComboBoxModel.clear();
  }

  public SwingEntityComboBoxModel getEntityComboBoxModel() {
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
        setUpperAsSelected();
      }
      finally {
        setUpdatingModel(false);
      }
    });

    entityComboBoxModel.addRefreshListener(this::setUpperAsSelected);
  }

  private void setUpperAsSelected() {
    final Object upper = getUpperBound();
    if (upper instanceof Collection) {
      final Collection upperCollection = (Collection) upper;
      entityComboBoxModel.setSelectedItem(upperCollection.isEmpty() ? null : upperCollection.iterator().next());
    }
    else {
      entityComboBoxModel.setSelectedItem(upper);
    }
  }
}
