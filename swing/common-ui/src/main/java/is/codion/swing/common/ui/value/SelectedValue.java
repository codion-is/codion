/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class SelectedValue<V, C extends JComboBox<V>> extends AbstractComponentValue<V, C> {

  SelectedValue(final C comboBox) {
    super(comboBox);
    if (comboBox.getModel() instanceof ItemComboBoxModel) {
      throw new IllegalArgumentException("comboBox() does not support ItemComboBoxModel, use itemComboBox()");
    }
    comboBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyValueChange();
      }
    });
  }

  @Override
  protected V getComponentValue(final C component) {
    final ComboBoxModel<V> comboBoxModel = component.getModel();
    if (comboBoxModel instanceof FilteredComboBoxModel) {
      return ((FilteredComboBoxModel<V>) comboBoxModel).getSelectedValue();
    }

    return (V) comboBoxModel.getSelectedItem();
  }

  @Override
  protected void setComponentValue(final C component, final V value) {
    component.getModel().setSelectedItem(value);
  }
}
