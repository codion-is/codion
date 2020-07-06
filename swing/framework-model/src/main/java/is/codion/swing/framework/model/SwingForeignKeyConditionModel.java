/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Entity;
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
  public SwingForeignKeyConditionModel(final ForeignKeyProperty property, final SwingEntityComboBoxModel comboBoxModel) {
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
        setEqualValue(selected);
      }
    });
    addEqualsValueListener(() -> {
      try {
        setUpdatingModel(true);
        setEqualsValueAsSelected();
      }
      finally {
        setUpdatingModel(false);
      }
    });

    entityComboBoxModel.addRefreshListener(this::setEqualsValueAsSelected);
  }

  private void setEqualsValueAsSelected() {
    final Collection<Entity> equalsValues = getEqualValues();
    entityComboBoxModel.setSelectedItem(equalsValues.isEmpty() ? null : equalsValues.iterator().next());
  }
}
