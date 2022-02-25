/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import static java.util.Objects.requireNonNull;

final class DefaultNumberSpinnerBuilder<T extends Number> extends AbstractSpinnerBuilder<T, NumberSpinnerBuilder<T>>
        implements NumberSpinnerBuilder<T> {

  private final Class<T> valueClass;

  private T minimum;
  private T maximum;
  private T stepSize;
  private boolean groupingUsed = true;
  private String decimalFormatPattern;

  DefaultNumberSpinnerBuilder(SpinnerNumberModel spinnerNumberModel, Class<T> valueClass,
                              Value<T> linkedValue) {
    super(spinnerNumberModel, linkedValue);
    this.valueClass = requireNonNull(valueClass);
    if (!valueClass.equals(Integer.class) && !valueClass.equals(Double.class)) {
      throw new IllegalStateException("NumberSpinnerBuilder not implemented for type: " + valueClass);
    }
  }

  @Override
  public NumberSpinnerBuilder<T> minimum(T minimum) {
    this.minimum = minimum;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> maximum(T maximum) {
    this.maximum = maximum;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> stepSize(T stepSize) {
    this.stepSize = stepSize;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> groupingUsed(boolean groupingUsed) {
    this.groupingUsed = groupingUsed;
    return this;
  }

  @Override
  public NumberSpinnerBuilder<T> decimalFormatPattern(String decimalFormatPattern) {
    this.decimalFormatPattern = requireNonNull(decimalFormatPattern);
    return this;
  }

  @Override
  protected ComponentValue<T, JSpinner> buildComponentValue(JSpinner component) {
    if (valueClass.equals(Integer.class)) {
      return (ComponentValue<T, JSpinner>) ComponentValues.integerSpinner(component);
    }
    if (valueClass.equals(Double.class)) {
      return (ComponentValue<T, JSpinner>) ComponentValues.doubleSpinner(component);
    }

    throw new IllegalStateException("NumberSpinnerBuilder not implemented for type: " + valueClass);
  }

  @Override
  protected JSpinner createSpinner() {
    SpinnerNumberModel spinnerNumberModel = (SpinnerNumberModel) spinnerModel;
    if (minimum != null) {
      spinnerNumberModel.setMinimum((Comparable<T>) minimum);
    }
    if (maximum != null) {
      spinnerNumberModel.setMaximum((Comparable<T>) maximum);
    }
    if (stepSize != null) {
      spinnerNumberModel.setStepSize(stepSize);
    }
    JSpinner spinner = super.createSpinner();
    JSpinner.NumberEditor numberEditor = decimalFormatPattern == null ?
            new JSpinner.NumberEditor(spinner) : new JSpinner.NumberEditor(spinner, decimalFormatPattern);
    numberEditor.getFormat().setGroupingUsed(groupingUsed);
    spinner.setEditor(numberEditor);

    return spinner;
  }
}
