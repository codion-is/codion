/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<T>, ComboBoxBuilder<T>>
        implements ComboBoxBuilder<T> {

  private final ComboBoxModel<T> comboBoxModel;
  private final Class<T> valueClass;

  private boolean editable = false;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();
  private ListCellRenderer<T> renderer;

  DefaultComboBoxBuilder(final Class<T> valueClass, final ComboBoxModel<T> comboBoxModel, final Value<T> linkedValue) {
    super(linkedValue);
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
  public ComboBoxBuilder<T> renderer(final ListCellRenderer<T> renderer) {
    this.renderer = requireNonNull(renderer);
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
    if (renderer != null) {
      comboBox.setRenderer(renderer);
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
    TransferFocusOnEnter.enable((JComponent) component.getEditor().getEditorComponent());
  }

  @Override
  protected void setInitialValue(final SteppedComboBox<T> component, final T initialValue) {
    component.setSelectedItem(initialValue);
  }
}
