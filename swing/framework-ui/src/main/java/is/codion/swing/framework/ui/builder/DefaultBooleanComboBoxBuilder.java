/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

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
  public SteppedComboBox<Item<Boolean>> build() {
    final SteppedComboBox<Item<Boolean>> comboBox = createBooleanComboBox();
    setPreferredSize(comboBox);
    onBuild(comboBox);
    comboBox.setTransferFocusOnEnter(transferFocusOnEnter);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter((JComponent) comboBox.getEditor().getEditorComponent());
    }

    return comboBox;
  }

  private SteppedComboBox<Item<Boolean>> createBooleanComboBox() {
    final BooleanComboBoxModel comboBoxModel = new BooleanComboBoxModel();
    final SteppedComboBox<Item<Boolean>> comboBox = new SteppedComboBox<>(comboBoxModel);
    ComponentValues.itemComboBox(comboBox).link(value);
    DefaultComboBoxBuilder.addComboBoxCompletion(comboBox);
    comboBox.setPopupWidth(BOOLEAN_COMBO_BOX_POPUP_WIDTH);

    return setDescriptionAndEnabledState(comboBox, property.getDescription(), enabledState);
  }
}
