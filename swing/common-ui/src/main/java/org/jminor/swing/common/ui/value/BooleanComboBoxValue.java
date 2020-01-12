/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Item;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

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

  @Override
  protected Boolean getComponentValue(final JComboBox component) {
    return (Boolean) ((Item) component.getModel().getSelectedItem()).getValue();
  }

  @Override
  protected void setComponentValue(final JComboBox component, final Boolean value) {
    component.setSelectedItem(value);
  }
}
