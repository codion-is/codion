/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.model.Item;
import org.jminor.common.model.combobox.BooleanComboBoxModel;

import javax.swing.JComboBox;

/**
 * A InputProvider implementation for boolean values.
 */
public final class BooleanInputProvider extends AbstractInputProvider<Boolean, JComboBox> {

  public BooleanInputProvider(final Boolean currentValue) {
    super(new JComboBox(new BooleanComboBoxModel()));
    if (currentValue != null) {
      getInputComponent().getModel().setSelectedItem(currentValue);
    }
  }

  @Override
  public Boolean getValue() {
    return (Boolean) ((Item) getInputComponent().getModel().getSelectedItem()).getItem();
  }
}
