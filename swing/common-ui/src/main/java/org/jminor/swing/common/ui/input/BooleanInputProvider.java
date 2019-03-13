/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.Item;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;

import javax.swing.JComboBox;

/**
 * A InputProvider implementation for boolean values.
 */
public final class BooleanInputProvider extends AbstractInputProvider<Boolean, JComboBox> {

  /**
   * Instantiates a new BooleanInputProvider.
   * @param initialValue the initial value
   */
  public BooleanInputProvider(final Boolean initialValue) {
    super(new JComboBox<>(new BooleanComboBoxModel()));
    getInputComponent().getModel().setSelectedItem(initialValue);
  }

  /** {@inheritDoc} */
  @Override
  public Boolean getValue() {
    return (Boolean) ((Item) getInputComponent().getModel().getSelectedItem()).getItem();
  }
}
