/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import static java.util.Objects.requireNonNull;

final class DefaultNumberSpinnerBuilder<T extends Number> extends AbstractSpinnerBuilder<T, NumberSpinnerBuilder<T>>
        implements NumberSpinnerBuilder<T> {

  private final Class<T> valueClass;

  private T minimum;
  private T maximum;
  private T stepSize;

  DefaultNumberSpinnerBuilder(final SpinnerNumberModel spinnerNumberModel, final Class<T> valueClass) {
    super(spinnerNumberModel);
    this.valueClass = requireNonNull(valueClass);
    if (!valueClass.equals(Integer.class) && !valueClass.equals(Double.class)) {
      throw new IllegalStateException("NumberSpinnerBuilder not implemented for type: " + valueClass);
    }
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
  protected JSpinner buildComponent() {
    final SpinnerNumberModel spinnerNumberModel = (SpinnerNumberModel) spinnerModel;
    if (minimum != null) {
      spinnerNumberModel.setMinimum((Comparable<T>) minimum);
    }
    if (maximum != null) {
      spinnerNumberModel.setMaximum((Comparable<T>) maximum);
    }
    if (stepSize != null) {
      spinnerNumberModel.setStepSize(stepSize);
    }

    return super.buildComponent();
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
}
