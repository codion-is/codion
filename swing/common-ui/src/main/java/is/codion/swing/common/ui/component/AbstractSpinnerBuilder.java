/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.spinner.SpinnerMouseWheelListener;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;

import static java.util.Objects.requireNonNull;

abstract class AbstractSpinnerBuilder<T, B extends SpinnerBuilder<T, B>> extends AbstractComponentBuilder<T, JSpinner, B>
        implements SpinnerBuilder<T, B> {

  protected final SpinnerModel spinnerModel;

  private boolean editable = true;
  private int columns = 0;
  private boolean mouseWheelScrolling = true;
  private boolean mouseWheelScrollingReversed = false;
  private int horizontalAlignment = -1;

  protected AbstractSpinnerBuilder(final SpinnerModel spinnerModel, final Value<T> linkedValue) {
    super(linkedValue);
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
    if (mouseWheelScrolling) {
      this.mouseWheelScrollingReversed = false;
    }
    return (B) this;
  }

  @Override
  public B mouseWheelScrollingReversed(final boolean mouseWheelScrollingReversed) {
    this.mouseWheelScrollingReversed = mouseWheelScrollingReversed;
    if (mouseWheelScrollingReversed) {
      this.mouseWheelScrolling = false;
    }
    return (B) this;
  }

  @Override
  public final B horizontalAlignment(final int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return (B) this;
  }

  @Override
  protected final JSpinner buildComponent() {
    JSpinner spinner = createSpinner();
    JComponent editor = spinner.getEditor();
    if (editor instanceof JSpinner.DefaultEditor) {
      JTextField editorField = ((JSpinner.DefaultEditor) editor).getTextField();
      if (!editable) {
        editorField.setEditable(false);
      }
      if (columns > 0) {
        editorField.setColumns(columns);
      }
      if (horizontalAlignment != -1) {
        editorField.setHorizontalAlignment(horizontalAlignment);
      }
    }
    if (mouseWheelScrolling) {
      spinner.addMouseWheelListener(SpinnerMouseWheelListener.create(spinnerModel));
    }
    if (mouseWheelScrollingReversed) {
      spinner.addMouseWheelListener(SpinnerMouseWheelListener.createReversed(spinnerModel));
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
    TransferFocusOnEnter.enable(((JSpinner.DefaultEditor) component.getEditor()).getTextField());
  }

  protected JSpinner createSpinner() {
    return new JSpinner(spinnerModel);
  }
}
