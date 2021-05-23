/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

final class DefaultComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<T>, ComboBoxBuilder<T>>
        implements ComboBoxBuilder<T> {

  private final ComboBoxModel<T> comboBoxModel;
  private final Class<T> valueClass;

  private boolean editable = false;

  DefaultComboBoxBuilder(final Value<T> value, final Class<T> valueClass, final ComboBoxModel<T> comboBoxModel) {
    super(value);
    this.valueClass = valueClass;
    this.comboBoxModel = comboBoxModel;
    preferredHeight(TextFields.getPreferredTextFieldHeight());
  }

  @Override
  public ComboBoxBuilder<T> editable(final boolean editable) {
    if (editable && !valueClass.equals(String.class)) {
      throw new IllegalArgumentException("Editable ComboBox is only supported for String values");
    }
    this.editable = editable;
    return this;
  }

  @Override
  protected SteppedComboBox<T> buildComponent() {
    final SteppedComboBox<T> comboBox = new SteppedComboBox<>(comboBoxModel);
    comboBox.setEditable(editable);
    ComponentValues.comboBox(comboBox).link(value);

    return comboBox;
  }

  @Override
  protected void setTransferFocusOnEnter(final SteppedComboBox<T> component) {
    component.setTransferFocusOnEnter(true);
    Components.transferFocusOnEnter((JComponent) component.getEditor().getEditorComponent());
  }

  static void addComboBoxCompletion(final JComboBox<?> comboBox) {
    final String completionMode = EntityInputComponents.COMBO_BOX_COMPLETION_MODE.get();
    switch (completionMode) {
      case Completion.COMPLETION_MODE_NONE:
        break;
      case Completion.COMPLETION_MODE_AUTOCOMPLETE:
        Completion.autoComplete(comboBox);
        break;
      case Completion.COMPLETION_MODE_MAXIMUM_MATCH:
        Completion.maximumMatch(comboBox);
        break;
      default:
        throw new IllegalArgumentException("Unknown completion mode: " + completionMode);
    }
  }
}
