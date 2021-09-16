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
import javax.swing.SpinnerNumberModel;

import static java.util.Objects.requireNonNull;

final class DefaultNumberSpinnerBuilder<T extends Number> extends AbstractComponentBuilder<T, JSpinner, NumberSpinnerBuilder<T>> implements NumberSpinnerBuilder<T> {

  private final SpinnerNumberModel spinnerNumberModel;
  private final Class<T> valueClass;

  private int columns = 0;
  private boolean editable = true;
  private T minimum;
  private T maximum;
  private T stepSize;
  private boolean mouseWheelScrolling = false;

  DefaultNumberSpinnerBuilder(final SpinnerNumberModel spinnerNumberModel, final Class<T> valueClass) {
    this.spinnerNumberModel = requireNonNull(spinnerNumberModel);
    this.valueClass = requireNonNull(valueClass);
    if (!valueClass.equals(Integer.class) && !valueClass.equals(Double.class)) {
      throw new IllegalStateException("NumberSpinnerBuilder not implemented for type: " + valueClass);
    }
  }

  @Override
  public NumberSpinnerBuilder<T> columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> editable(final boolean editable) {
    this.editable = editable;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> minimum(final T minimum) {
    this.minimum = minimum;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> maximum(final T maximum) {
    this.maximum = maximum;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> stepSize(final T stepSize) {
    this.stepSize = stepSize;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> mouseWheelScrolling(final boolean mouseWheelScrolling) {
    this.mouseWheelScrolling = mouseWheelScrolling;
    return this;
  }

  @Override
  protected JSpinner buildComponent() {
    if (minimum != null) {
      spinnerNumberModel.setMinimum((Comparable<T>) minimum);
    }
    if (maximum != null) {
      spinnerNumberModel.setMaximum((Comparable<T>) maximum);
    }
    if (stepSize != null) {
      spinnerNumberModel.setStepSize(stepSize);
    }
    final JSpinner spinner = new JSpinner(spinnerNumberModel);
    final JComponent editor = spinner.getEditor();
    if (editor instanceof JSpinner.DefaultEditor) {
      final JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor) editor;
      if (columns > 0) {
        defaultEditor.getTextField().setColumns(columns);
      }
      if (!editable) {
        defaultEditor.getTextField().setEditable(false);
      }
    }
    if (mouseWheelScrolling) {
      spinner.addMouseWheelListener(new SpinnerMouseWheelListener(spinnerNumberModel));
    }

    return spinner;
  }

  @Override
  protected ComponentValue<T, JSpinner> buildComponentValue(final JSpinner component) {
    if (valueClass.equals(Integer.class)) {
      return (ComponentValue<T, JSpinner>) ComponentValues.integerSpinner(component);
    }
    if (valueClass.equals(Double.class)) {
      return (ComponentValue<T, JSpinner>) ComponentValues.doubleSpinner(component);
    }

    throw new IllegalStateException("NumberSpinnerBuilder not implemented for type: " + valueClass);
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
