/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import static java.util.Objects.requireNonNull;

final class DefaultSpinnerBuilder<T extends Number> extends AbstractComponentBuilder<T, JSpinner, SpinnerBuilder<T>> implements SpinnerBuilder<T> {

  private final SpinnerNumberModel spinnerNumberModel;
  private final Class<T> valueClass;

  private int columns = 0;
  private boolean editable = true;
  private T minimum;
  private T maximum;
  private T stepSize;

  DefaultSpinnerBuilder(final SpinnerNumberModel spinnerNumberModel, final Class<T> valueClass) {
    this.spinnerNumberModel = requireNonNull(spinnerNumberModel);
    this.valueClass = requireNonNull(valueClass);
    if (!valueClass.equals(Integer.class) && !valueClass.equals(Double.class)) {
      throw new IllegalStateException("SpinnerBuilder not implemented for type: " + valueClass);
    }
  }

  @Override
  public SpinnerBuilder<T> columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public SpinnerBuilder<T> editable(final boolean editable) {
    this.editable = editable;
    return this;
  }

  @Override
  public SpinnerBuilder<T> minimum(final T minimum) {
    this.minimum = minimum;
    return this;
  }

  @Override
  public SpinnerBuilder<T> maximum(final T maximum) {
    this.maximum = maximum;
    return this;
  }

  @Override
  public SpinnerBuilder<T> stepSize(final T stepSize) {
    this.stepSize = stepSize;
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

    throw new IllegalStateException("SpinnerBuilder not implemented for type: " + valueClass);
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
