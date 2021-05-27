/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import static java.util.Objects.requireNonNull;

final class DefaultSpinnerBuilder<T extends Number> extends AbstractComponentBuilder<T, JSpinner, SpinnerBuilder<T>> implements SpinnerBuilder<T> {

  private final SpinnerNumberModel spinnerNumberModel;
  private final Class<T> valueClass;
  private int columns = 0;

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
  protected JSpinner buildComponent() {
    final JSpinner spinner = new JSpinner(spinnerNumberModel);
    if (columns > 0) {
      ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
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
}
