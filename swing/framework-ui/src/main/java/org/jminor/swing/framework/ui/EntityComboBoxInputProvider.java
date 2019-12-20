/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.swing.common.ui.input.AbstractInputProvider;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;

/**
 * A InputProvider implementation for Entity values based on a {@link EntityComboBox}.
 * @see SwingEntityComboBoxModel
 */
public final class EntityComboBoxInputProvider extends AbstractInputProvider<Entity, EntityComboBox> {

  /**
   * Instantiates a new input provider based on the EntityComboBoxModel class
   * @param comboBoxModel the combo box model
   * @param initialValue the initial value to display
   */
  public EntityComboBoxInputProvider(final SwingEntityComboBoxModel comboBoxModel, final Entity initialValue) {
    super(createComboBox(comboBoxModel, initialValue));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getValue() {
    return getInputComponent().getModel().getSelectedValue();
  }

  /** {@inheritDoc} */
  @Override
  public void setValue(final Entity value) {
    getInputComponent().setSelectedItem(value);
  }

  private static EntityComboBox createComboBox(final SwingEntityComboBoxModel comboBoxModel, final Object currentValue) {
    if (comboBoxModel.isCleared()) {
      comboBoxModel.refresh();
    }
    if (currentValue != null) {
      comboBoxModel.setSelectedItem(currentValue);
    }

    return new EntityComboBox(comboBoxModel);
  }
}
