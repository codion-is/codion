/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JComponent;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultBooleanComboBoxBuilder extends AbstractComponentBuilder<Boolean, SteppedComboBox<Item<Boolean>>, BooleanComboBoxBuilder>
        implements BooleanComboBoxBuilder {

  private final ItemComboBoxModel<Boolean> comboBoxModel;

  DefaultBooleanComboBoxBuilder(final ItemComboBoxModel<Boolean> comboBoxModel, final Value<Boolean> linkedValue) {
    super(linkedValue);
    this.comboBoxModel = requireNonNull(comboBoxModel);
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  protected SteppedComboBox<Item<Boolean>> buildComponent() {
    return Completion.enable(new SteppedComboBox<>(comboBoxModel));
  }

  @Override
  protected ComponentValue<Boolean, SteppedComboBox<Item<Boolean>>> buildComponentValue(final SteppedComboBox<Item<Boolean>> component) {
    return ComponentValues.itemComboBox(component);
  }

  @Override
  protected void setTransferFocusOnEnter(final SteppedComboBox<Item<Boolean>> component) {
    component.setTransferFocusOnEnter(true);
    TransferFocusOnEnter.enable((JComponent) component.getEditor().getEditorComponent());
  }

  @Override
  protected void setInitialValue(final SteppedComboBox<Item<Boolean>> component, final Boolean initialValue) {
    component.setSelectedItem(initialValue);
  }
}
