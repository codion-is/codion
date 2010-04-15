/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;

import javax.swing.JComboBox;

/**
   * A InputManager implementation for boolean values.
 */
public class BooleanInputProvider extends AbstractInputProvider<Boolean> {

  public BooleanInputProvider(final Boolean currentValue) {
    super(new JComboBox(new BooleanComboBoxModel()));
    if (currentValue != null)
      ((JComboBox) getInputComponent()).getModel().setSelectedItem(currentValue);
  }

  @Override
  public Boolean getValue() {
    return (Boolean) ((ItemComboBoxModel.Item) ((JComboBox) getInputComponent()).getModel().getSelectedItem()).getItem();
  }
}
