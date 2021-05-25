/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JComponent;
import java.util.List;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

final class DefaultValueListComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<Item<T>>, ValueListComboBoxBuilder<T>>
        implements ValueListComboBoxBuilder<T> {

  private final List<Item<T>> values;

  private int popupWidth;
  private boolean sorted = true;
  private boolean nullable;

  DefaultValueListComboBoxBuilder(final List<Item<T>> values) {
    this.values = values;
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public ValueListComboBoxBuilder<T> nullable(final boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  public ValueListComboBoxBuilder<T> popupWidth(final int popupWidth) {
    this.popupWidth = popupWidth;
    return this;
  }

  @Override
  public ValueListComboBoxBuilder<T> sorted(final boolean sorted) {
    this.sorted = sorted;
    return this;
  }

  @Override
  protected SteppedComboBox<Item<T>> buildComponent() {
    final ItemComboBoxModel<T> valueListComboBoxModel = createValueListComboBoxModel();
    final SteppedComboBox<Item<T>> comboBox = new SteppedComboBox<>(valueListComboBoxModel);
    Completion.enableComboBoxCompletion(comboBox);
    if (popupWidth > 0) {
      comboBox.setPopupWidth(popupWidth);
    }

    return comboBox;
  }

  @Override
  protected ComponentValue<T, SteppedComboBox<Item<T>>> buildComponentValue(final SteppedComboBox<Item<T>> component) {
    return ComponentValues.itemComboBox(component);
  }

  @Override
  protected void setTransferFocusOnEnter(final SteppedComboBox<Item<T>> component) {
    component.setTransferFocusOnEnter(true);
    Components.transferFocusOnEnter((JComponent) component.getEditor().getEditorComponent());
  }

  @Override
  protected void setInitialValue(final SteppedComboBox<Item<T>> component, final T initialValue) {
    component.setSelectedItem(initialValue);
  }

  private ItemComboBoxModel<T> createValueListComboBoxModel() {
    final ItemComboBoxModel<T> model = sorted ?
            new ItemComboBoxModel<>(values) : new ItemComboBoxModel<>(null, values);
    final Item<T> nullItem = Item.item(null, FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    if (nullable && !model.containsItem(nullItem)) {
      model.addItem(nullItem);
      model.setSelectedItem(nullItem);
    }

    return model;
  }
}
