/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Item;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

/**
 * A InputProvider implementation for boolean values.
 */
final class BooleanComboBoxValue extends AbstractComponentValue<Boolean, JComboBox> {

  /**
   * Instantiates a new BooleanInputProvider.
   * @param initialValue the initial value
   */
  BooleanComboBoxValue(final Boolean initialValue) {
    super(new JComboBox<>(new BooleanComboBoxModel()));
    getComponent().getModel().setSelectedItem(initialValue);
    getComponent().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange(get());
      }
    });
  }

  /** {@inheritDoc} */
  @Override
  public Boolean get() {
    return (Boolean) ((Item) getComponent().getModel().getSelectedItem()).getValue();
  }

  /** {@inheritDoc} */
  @Override
  protected void setInternal(final Boolean value) {
    getComponent().setSelectedItem(value);
  }
}
