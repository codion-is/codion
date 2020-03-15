/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.item.Item;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class SelectedValue<V> extends AbstractComponentValue<V, JComboBox<V>> {

  SelectedValue(final JComboBox<V> comboBox) {
    super(comboBox);
    comboBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected V getComponentValue(final JComboBox<V> component) {
    final ComboBoxModel<V> comboBoxModel = component.getModel();
    if (comboBoxModel instanceof ItemComboBoxModel) {
      return (V) ((Item) comboBoxModel.getSelectedItem()).getValue();
    }
    else if (comboBoxModel instanceof FilteredComboBoxModel) {
      return (V) ((FilteredComboBoxModel) comboBoxModel).getSelectedValue();
    }

    return (V) comboBoxModel.getSelectedItem();
  }

  @Override
  protected void setComponentValue(final JComboBox<V> component, final V value) {
    component.getModel().setSelectedItem(value);
  }
}
