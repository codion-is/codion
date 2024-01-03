/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

final class SelectedValue<T, C extends JComboBox<T>> extends AbstractComponentValue<T, C> {

  SelectedValue(C comboBox) {
    super(comboBox);
    if (comboBox.getModel() instanceof FilteredComboBoxModel) {
      //ItemListener does not get notified when null values are selected/deselected
      ((FilteredComboBoxModel<T>) comboBox.getModel()).addSelectionListener(selected -> notifyListeners());
    }
    else {
      comboBox.addItemListener(new NotifyOnItemSelectedListener());
    }
  }

  @Override
  protected T getComponentValue() {
    ComboBoxModel<T> comboBoxModel = component().getModel();
    if (comboBoxModel instanceof FilteredComboBoxModel) {
      return ((FilteredComboBoxModel<T>) comboBoxModel).selectedValue();
    }

    return (T) comboBoxModel.getSelectedItem();
  }

  @Override
  protected void setComponentValue(T value) {
    component().getModel().setSelectedItem(value);
  }

  private final class NotifyOnItemSelectedListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        notifyListeners();
      }
    }
  }
}
