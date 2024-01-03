/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

import static java.util.Objects.requireNonNull;

final class DefaultNumberSpinnerBuilder<T extends Number> extends AbstractSpinnerBuilder<T, NumberSpinnerBuilder<T>>
        implements NumberSpinnerBuilder<T> {

  private final Class<T> valueClass;

  private T minimum;
  private T maximum;
  private T stepSize;
  private boolean groupingUsed = true;
  private String decimalFormatPattern;
  boolean commitOnValidEdit = true;

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
  public NumberSpinnerBuilder<T> commitOnValidEdit(boolean commitOnValidEdit) {
    this.commitOnValidEdit = commitOnValidEdit;
    return this;
  }

  @Override
  protected ComponentValue<T, JSpinner> createComponentValue(JSpinner component) {
    if (valueClass.equals(Integer.class) || valueClass.equals(Double.class)) {
      return new SpinnerNumberValue<>(component);
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
    if (commitOnValidEdit) {
      ((DefaultFormatter) numberEditor.getTextField().getFormatter()).setCommitsOnValidEdit(true);
    }
    spinner.setEditor(numberEditor);

    return spinner;
  }
}
