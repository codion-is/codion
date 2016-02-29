/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.swing.common.ui.input.AbstractInputProvider;
import org.jminor.swing.framework.model.EntityComboBoxModel;

import javax.swing.JComboBox;

/**
 * A InputProvider implementation for Entity values based on a JComboBox.
 * @see EntityComboBoxModel
 */
public final class EntityComboProvider extends AbstractInputProvider<Entity, JComboBox<Entity>> {

  /**
   * Instantiates a new input provider based on the EntityComboBoxModel class
   * @param comboBoxModel the combo box model
   * @param initialValue the initial value to display
   */
  public EntityComboProvider(final EntityComboBoxModel comboBoxModel, final Entity initialValue) {
    super(createComboBox(comboBoxModel, initialValue));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getValue() {
    return ((EntityComboBoxModel) getInputComponent().getModel()).getSelectedValue();
  }

  private static JComboBox<Entity> createComboBox(final EntityComboBoxModel comboBoxModel, final Object currentValue) {
    if (comboBoxModel.isCleared()) {
      comboBoxModel.refresh();
    }
    if (currentValue != null) {
      comboBoxModel.setSelectedItem(currentValue);
    }

    return new JComboBox<>(comboBoxModel);
  }
}
