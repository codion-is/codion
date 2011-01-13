/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.input.AbstractInputProvider;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.domain.Entity;

import javax.swing.JComboBox;

/**
 * A InputProvider implementation for Entity values based on a JComboBox.
 * @see EntityComboBoxModel
 */
public final class EntityComboProvider extends AbstractInputProvider<Entity, JComboBox> {

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

  private static JComboBox createComboBox(final EntityComboBoxModel comboBoxModel, final Object currentValue) {
    if (comboBoxModel.isCleared()) {
      comboBoxModel.refresh();
    }
    if (currentValue != null) {
      comboBoxModel.setSelectedItem(currentValue);
    }

    return new JComboBox(comboBoxModel);
  }
}
