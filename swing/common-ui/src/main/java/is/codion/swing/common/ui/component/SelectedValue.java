/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;

final class SelectedValue<T, C extends JComboBox<T>> extends AbstractComponentValue<T, C> {

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
  protected T getComponentValue(final C component) {
    ComboBoxModel<T> comboBoxModel = component.getModel();
    if (comboBoxModel instanceof FilteredComboBoxModel) {
      return ((FilteredComboBoxModel<T>) comboBoxModel).getSelectedValue();
    }

    return (T) comboBoxModel.getSelectedItem();
  }

  @Override
  protected void setComponentValue(final C component, final T value) {
    component.getModel().setSelectedItem(value);
  }
}
