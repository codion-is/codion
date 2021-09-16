/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.spinner.SpinnerMouseWheelListener;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

import static java.util.Objects.requireNonNull;

final class DefaultItemSpinnerBuilder<T> extends AbstractComponentBuilder<T, JSpinner, ItemSpinnerBuilder<T>> implements ItemSpinnerBuilder<T> {

  private final SpinnerListModel spinnerModel;

  private int columns = 0;
  private boolean mouseWheelScrolling = false;

  DefaultItemSpinnerBuilder(final SpinnerListModel spinnerModel) {
    this.spinnerModel = requireNonNull(spinnerModel);
  }

  @Override
  public ItemSpinnerBuilder<T> columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public ItemSpinnerBuilder<T> mouseWheelScrolling(final boolean mouseWheelScrolling) {
    this.mouseWheelScrolling = mouseWheelScrolling;
    return this;
  }

  @Override
  protected JSpinner buildComponent() {
    final JSpinner spinner = new JSpinner(spinnerModel);
    final JComponent editor = spinner.getEditor();
    if (editor instanceof JSpinner.DefaultEditor) {
      final JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor) editor;
      if (columns > 0) {
        defaultEditor.getTextField().setColumns(columns);
      }
    }
    if (mouseWheelScrolling) {
      spinner.addMouseWheelListener(new SpinnerMouseWheelListener(spinnerModel));
    }

    return spinner;
  }

  @Override
  protected ComponentValue<T, JSpinner> buildComponentValue(final JSpinner component) {
    return ComponentValues.itemSpinner(component);
  }

  @Override
  protected void setInitialValue(final JSpinner component, final T initialValue) {
    component.setValue(initialValue);
  }

  @Override
  protected void setTransferFocusOnEnter(final JSpinner component) {
    super.setTransferFocusOnEnter(component);
    Components.transferFocusOnEnter(((JSpinner.DefaultEditor) component.getEditor()).getTextField());
  }
}
