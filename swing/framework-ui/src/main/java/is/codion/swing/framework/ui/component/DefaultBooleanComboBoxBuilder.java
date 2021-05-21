/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JComponent;

final class DefaultBooleanComboBoxBuilder extends AbstractComponentBuilder<Boolean, SteppedComboBox<Item<Boolean>>, BooleanComboBoxBuilder>
        implements BooleanComboBoxBuilder {

  private static final int BOOLEAN_COMBO_BOX_POPUP_WIDTH = 40;

  DefaultBooleanComboBoxBuilder(final Property<Boolean> attribute, final Value<Boolean> value) {
    super(attribute, value);
  }

  @Override
  protected SteppedComboBox<Item<Boolean>> buildComponent() {
    final BooleanComboBoxModel comboBoxModel = new BooleanComboBoxModel();
    final SteppedComboBox<Item<Boolean>> comboBox = new SteppedComboBox<>(comboBoxModel);
    ComponentValues.itemComboBox(comboBox).link(value);
    DefaultComboBoxBuilder.addComboBoxCompletion(comboBox);
    comboBox.setPopupWidth(BOOLEAN_COMBO_BOX_POPUP_WIDTH);

    return comboBox;
  }

  @Override
  protected void setTransferFocusOnEnter(final SteppedComboBox<Item<Boolean>> component) {
    component.setTransferFocusOnEnter(true);
    Components.transferFocusOnEnter((JComponent) component.getEditor().getEditorComponent());
  }
}
