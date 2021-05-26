/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<T>, ComboBoxBuilder<T>>
        implements ComboBoxBuilder<T> {

  private final ComboBoxModel<T> comboBoxModel;
  private final Class<T> valueClass;

  private boolean editable = false;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();

  DefaultComboBoxBuilder(final Class<T> valueClass, final ComboBoxModel<T> comboBoxModel) {
    this.valueClass = valueClass;
    this.comboBoxModel = comboBoxModel;
    preferredHeight(getPreferredTextFieldHeight());
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
  public ComboBoxBuilder<T> completionMode(final Completion.Mode completionMode) {
    this.completionMode = requireNonNull(completionMode);
    return this;
  }

  @Override
  protected SteppedComboBox<T> buildComponent() {
    final SteppedComboBox<T> comboBox = new SteppedComboBox<>(comboBoxModel);
    if (editable) {
      comboBox.setEditable(true);
    }
    else {
      Completion.enable(comboBox, completionMode);
    }

    return comboBox;
  }

  @Override
  protected ComponentValue<T, SteppedComboBox<T>> buildComponentValue(final SteppedComboBox<T> component) {
    return ComponentValues.comboBox(component);
  }

  @Override
  protected void setTransferFocusOnEnter(final SteppedComboBox<T> component) {
    component.setTransferFocusOnEnter(true);
    Components.transferFocusOnEnter((JComponent) component.getEditor().getEditorComponent());
  }

  @Override
  protected void setInitialValue(final SteppedComboBox<T> component, final T initialValue) {
    component.setSelectedItem(initialValue);
  }
}
