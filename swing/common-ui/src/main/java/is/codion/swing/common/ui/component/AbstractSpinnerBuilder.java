/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.spinner.SpinnerMouseWheelListener;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;

import static java.util.Objects.requireNonNull;

abstract class AbstractSpinnerBuilder<T, B extends SpinnerBuilder<T, B>> extends AbstractComponentBuilder<T, JSpinner, B>
        implements SpinnerBuilder<T, B> {

  protected final SpinnerModel spinnerModel;

  private boolean editable = true;
  private int columns = 0;
  private boolean mouseWheelScrolling = false;

  protected AbstractSpinnerBuilder(final SpinnerModel spinnerModel) {
    this.spinnerModel = requireNonNull(spinnerModel);
  }

  @Override
  public final B editable(final boolean editable) {
    this.editable = editable;
    return (B) this;
  }

  @Override
  public final B columns(final int columns) {
    this.columns = columns;
    return (B) this;
  }

  @Override
  public final B mouseWheelScrolling(final boolean mouseWheelScrolling) {
    this.mouseWheelScrolling  = mouseWheelScrolling;
    return (B) this;
  }

  @Override
  protected JSpinner buildComponent() {
    final JSpinner spinner = new JSpinner(spinnerModel);
    final JComponent editor = spinner.getEditor();
    if (editor instanceof JSpinner.DefaultEditor) {
      final JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor) editor;
      if (!editable) {
        defaultEditor.getTextField().setEditable(false);
      }
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
  protected final void setInitialValue(final JSpinner component, final T initialValue) {
    component.setValue(initialValue);
  }

  @Override
  protected final void setTransferFocusOnEnter(final JSpinner component) {
    super.setTransferFocusOnEnter(component);
    Components.transferFocusOnEnter(((JSpinner.DefaultEditor) component.getEditor()).getTextField());
  }
}
