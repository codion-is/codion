/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.item.Item;
import is.codion.common.model.combobox.FilteredComboBoxModel;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JComponent;
import java.util.List;

final class DefaultValueListComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<Item<T>>, ValueListComboBoxBuilder<T>>
        implements ValueListComboBoxBuilder<T> {

  private boolean sorted = true;

  DefaultValueListComboBoxBuilder(final Property<T> attribute, final Value<T> value) {
    super(attribute, value);
  }

  @Override
  public ValueListComboBoxBuilder<T> sorted(final boolean sorted) {
    this.sorted = sorted;
    return this;
  }

  @Override
  public SteppedComboBox<Item<T>> build() {
    final SteppedComboBox<Item<T>> comboBox = createValueListComboBox();
    setPreferredSize(comboBox);
    onBuild(comboBox);
    comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }

    return comboBox;
  }

  private SteppedComboBox<Item<T>> createValueListComboBox() {
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property based on '" + property.getAttribute() + "' is not a " +
              "ValueListProperty");
    }
    final ItemComboBoxModel<T> valueListComboBoxModel = createValueListComboBoxModel();
    final SteppedComboBox<Item<T>> comboBox = new SteppedComboBox<>(valueListComboBoxModel);
    ComponentValues.itemComboBox(comboBox).link(value);
    DefaultComboBoxBuilder.addComboBoxCompletion(comboBox);

    return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
  }

  private ItemComboBoxModel<T> createValueListComboBoxModel() {
    final List<Item<T>> values = ((ValueListProperty<T>) property).getValues();
    final ItemComboBoxModel<T> model = sorted ?
            new ItemComboBoxModel<>(values) : new ItemComboBoxModel<>(null, values);
    final Item<T> nullItem = Item.item(null, FilteredComboBoxModel.COMBO_BOX_NULL_VALUE_ITEM.get());
    if (property.isNullable() && !model.containsItem(nullItem)) {
      model.addItem(nullItem);
      model.setSelectedItem(nullItem);
    }

    return model;
  }
}
