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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

import static java.util.Objects.requireNonNull;

final class DefaultNumberSpinnerBuilder<T extends Number> extends AbstractSpinnerBuilder<T, NumberSpinnerBuilder<T>>
				implements NumberSpinnerBuilder<T> {

	static final NumberClassStep NUMBER_CLASS = new DefaultNumberClassStep();

	private final Class<T> valueClass;

	private @Nullable T minimum;
	private @Nullable T maximum;
	private @Nullable T stepSize;
	private boolean groupingUsed = true;
	private @Nullable String decimalFormatPattern;
	boolean commitOnValidEdit = true;

	DefaultNumberSpinnerBuilder(Class<T> valueClass) {
		super(new  SpinnerNumberModel());
		this.valueClass = requireNonNull(valueClass);
		if (!valueClass.equals(Integer.class) && !valueClass.equals(Double.class)) {
			throw new IllegalStateException("NumberSpinnerBuilder not implemented for type: " + valueClass);
		}
	}

	@Override
	public NumberSpinnerBuilder<T> model(SpinnerModel model) {
		if (!(model instanceof SpinnerNumberModel)) {
			throw new IllegalArgumentException("model must be of type SpinnerNumberModel");
		}
		return super.model(model);
	}

	@Override
	public NumberSpinnerBuilder<T> minimum(@Nullable T minimum) {
		this.minimum = minimum;
		return this;
	}

	@Override
	public NumberSpinnerBuilder<T> maximum(@Nullable T maximum) {
		this.maximum = maximum;
		return this;
	}

	@Override
	public NumberSpinnerBuilder<T> stepSize(@Nullable T stepSize) {
		this.stepSize = stepSize;
		return this;
	}

	@Override
	public NumberSpinnerBuilder<T> groupingUsed(boolean groupingUsed) {
		this.groupingUsed = groupingUsed;
		return this;
	}

	@Override
	public NumberSpinnerBuilder<T> decimalFormatPattern(@Nullable String decimalFormatPattern) {
		this.decimalFormatPattern = decimalFormatPattern;
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
			return new SpinnerNumberValue<>(component, valueClass);
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

	private static final class DefaultNumberClassStep implements NumberClassStep {

		@Override
		public <T extends Number> NumberSpinnerBuilder<T> numberClass(Class<T> numberClass) {
			return new DefaultNumberSpinnerBuilder<>(numberClass);
		}
	}
}
