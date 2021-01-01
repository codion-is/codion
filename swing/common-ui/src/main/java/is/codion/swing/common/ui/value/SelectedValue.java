/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class SelectedValue<V> extends AbstractComponentValue<V, JComboBox<V>> {

  SelectedValue(final JComboBox<V> comboBox) {
    super(comboBox);
    if (comboBox.getModel() instanceof ItemComboBoxModel) {
      throw new IllegalArgumentException("SelectedValue does not support ItemComboBoxModel, use SelectedItemValue");
    }
    comboBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected V getComponentValue(final JComboBox<V> component) {
    final ComboBoxModel<V> comboBoxModel = component.getModel();
    if (comboBoxModel instanceof FilteredComboBoxModel) {
      return ((FilteredComboBoxModel<V>) comboBoxModel).getSelectedValue();
    }

    return (V) comboBoxModel.getSelectedItem();
  }

  @Override
  protected void setComponentValue(final JComboBox<V> component, final V value) {
    component.getModel().setSelectedItem(value);
  }
}
