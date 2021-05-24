/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JComponent;

final class DefaultBooleanComboBoxBuilder extends AbstractComponentBuilder<Boolean, SteppedComboBox<Item<Boolean>>, BooleanComboBoxBuilder>
        implements BooleanComboBoxBuilder {

  DefaultBooleanComboBoxBuilder() {
    preferredHeight(TextFields.getPreferredTextFieldHeight());
  }

  @Override
  protected SteppedComboBox<Item<Boolean>> buildComponent() {
    final BooleanComboBoxModel comboBoxModel = new BooleanComboBoxModel();
    final SteppedComboBox<Item<Boolean>> comboBox = new SteppedComboBox<>(comboBoxModel);
    Completion.addComboBoxCompletion(comboBox);

    return comboBox;
  }

  @Override
  protected ComponentValue<Boolean, SteppedComboBox<Item<Boolean>>> buildComponentValue(final SteppedComboBox<Item<Boolean>> component) {
    return ComponentValues.itemComboBox(component);
  }

  @Override
  protected void setTransferFocusOnEnter(final SteppedComboBox<Item<Boolean>> component) {
    component.setTransferFocusOnEnter(true);
    Components.transferFocusOnEnter((JComponent) component.getEditor().getEditorComponent());
  }
}
