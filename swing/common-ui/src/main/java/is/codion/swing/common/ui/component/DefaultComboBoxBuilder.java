/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.combobox.ComboBoxMouseWheelListener;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;
import static java.util.Objects.requireNonNull;

final class DefaultComboBoxBuilder<T> extends AbstractComponentBuilder<T, SteppedComboBox<T>, ComboBoxBuilder<T>>
        implements ComboBoxBuilder<T> {

  private final ComboBoxModel<T> comboBoxModel;

  private boolean editable = false;
  private Completion.Mode completionMode = Completion.COMBO_BOX_COMPLETION_MODE.get();
  private ListCellRenderer<T> renderer;
  private ComboBoxEditor editor;
  private boolean mouseWheelScrolling = false;

  DefaultComboBoxBuilder(final ComboBoxModel<T> comboBoxModel, final Value<T> linkedValue) {
    super(linkedValue);
    this.comboBoxModel = comboBoxModel;
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public ComboBoxBuilder<T> editable(final boolean editable) {
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
  public ComboBoxBuilder<T> editor(final ComboBoxEditor editor) {
    this.editor = requireNonNull(editor);
    return this;
  }

  @Override
  public ComboBoxBuilder<T> mouseWheelScrolling(final boolean mouseWheelScrolling) {
    this.mouseWheelScrolling = mouseWheelScrolling;
    return this;
  }

  @Override
  protected SteppedComboBox<T> buildComponent() {
    final SteppedComboBox<T> comboBox = new SteppedComboBox<>(comboBoxModel);
    if (renderer != null) {
      comboBox.setRenderer(renderer);
    }
    if (editor != null) {
      comboBox.setEditor(editor);
    }
    if (editable) {
      comboBox.setEditable(true);
    }
    if (!editable && editor == null) {
      Completion.enable(comboBox, completionMode);
    }
    if (mouseWheelScrolling) {
      comboBox.addMouseWheelListener(new ComboBoxMouseWheelListener(comboBoxModel));
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
