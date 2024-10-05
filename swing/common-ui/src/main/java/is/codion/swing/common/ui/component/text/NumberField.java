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
 * Copyright (c) 2015 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.Configuration;
import is.codion.common.observer.Mutable;
import is.codion.common.property.PropertyValue;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.text.NumberDocument.DecimalDocument;
import is.codion.swing.common.ui.component.text.NumberDocument.NumberParsingDocumentFilter;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.util.Objects.requireNonNull;

/**
 * A text field for numbers.
 * Use {@link #builder(Class)} or {@link #builder(Class, Value)} for {@link Builder} instances.
 * @param <T> the Number type
 */
public final class NumberField<T extends Number> extends HintTextField {

	/**
	 * Specifies whether NumberFields should convert a grouping separator symbol
	 * to a decimal separator symbol when typed. This solves the problem of locale
	 * controlling whether the numpad comma acts as a decimal symbol, which is usually what we want.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> CONVERT_GROUPING_TO_DECIMAL_SEPARATOR =
					Configuration.booleanValue(NumberField.class.getName() + ".convertGroupingToDecimalSeparator", true);

	private NumberField(AbstractNumberFieldBuilder<T> builder) {
		super(builder.createDocument());
		NumberDocument<T> document = document();
		document.setTextComponent(this);
		NumberParsingDocumentFilter<T> documentFilter = document.getDocumentFilter();
		documentFilter.setMinimumValue(builder.minimumValue);
		documentFilter.setMaximumValue(builder.maximumValue);
		documentFilter.setSilentValidation(builder.silentValidation);
		documentFilter.setConvertGroupingToDecimalSeparator(builder.convertGroupingToDecimalSeparator);
		if (document.getFormat() instanceof DecimalFormat) {
			addKeyListener(new GroupingSkipAdapter());
		}
		if (builder.groupingUsed != null) {
			document.setGroupingUsed(builder.groupingUsed);
		}
		if (builder.groupingSeparator != 0) {
			document.setGroupingSeparator(builder.groupingSeparator);
		}
		if (document instanceof DecimalDocument) {
			if (builder.maximumFractionDigits != -1) {
				((DecimalDocument<?>) document).setMaximumFractionDigits(builder.maximumFractionDigits);
			}
			if (builder.decimalSeparator != 0) {
				document.setDecimalSeparator(builder.decimalSeparator);
			}
		}
	}

	@Override
	public void setDocument(Document doc) {
		if (getDocument() instanceof NumberDocument) {
			throw new UnsupportedOperationException("NumberField document can not be set");
		}
		super.setDocument(doc);
	}

	/**
	 * Set whether grouping will be used in this field.
	 * @param groupingUsed true if grouping should be used false otherwise
	 */
	public void setGroupingUsed(boolean groupingUsed) {
		document().setGroupingUsed(groupingUsed);
	}

	/**
	 * Sets the range of values this field should allow
	 * @param minimumValue the minimum value
	 * @param maximumValue the maximum value
	 */
	public void setValueRange(Number minimumValue, Number maximumValue) {
		document().getDocumentFilter().setMinimumValue(minimumValue);
		document().getDocumentFilter().setMaximumValue(maximumValue);
	}

	/**
	 * @param minimumValue the minimum value
	 */
	public void setMinimumValue(Number minimumValue) {
		document().getDocumentFilter().setMinimumValue(minimumValue);
	}

	/**
	 * @return the minimum value this field should accept
	 */
	public Number getMinimumValue() {
		return document().getDocumentFilter().getMinimumValue();
	}

	/**
	 * @param maximumValue the maximum value
	 */
	public void setMaximumValue(Number maximumValue) {
		document().getDocumentFilter().setMaximumValue(maximumValue);
	}

	/**
	 * @return the maximum value this field should accept
	 */
	public Number getMaximumValue() {
		return document().getDocumentFilter().getMaximumValue();
	}

	/**
	 * Set the decimal and grouping separators for this field
	 * @param decimalSeparator the decimal separator
	 * @param groupingSeparator the grouping separator
	 * @throws IllegalArgumentException in case both separators are the same character
	 */
	public void setSeparators(char decimalSeparator, char groupingSeparator) {
		document().setSeparators(decimalSeparator, groupingSeparator);
	}

	/**
	 * Sets the decimal separator
	 * @param decimalSeparator the separator
	 */
	public void setDecimalSeparator(char decimalSeparator) {
		document().setDecimalSeparator(decimalSeparator);
	}

	/**
	 * Sets the grouping separator
	 * @param groupingSeparator the separator
	 */
	public void setGroupingSeparator(char groupingSeparator) {
		document().setGroupingSeparator(groupingSeparator);
	}

	/**
	 * @return the maximum number of fraction digits this field shows
	 * @throws IllegalStateException in case this NumberField is not based on a decimal type
	 */
	public int getMaximumFractionDigits() {
		NumberDocument<T> typedDocument = document();
		if (!(typedDocument instanceof DecimalDocument)) {
			throw new IllegalStateException("This is not a decimal based NumberField");
		}

		return ((DecimalDocument<BigDecimal>) typedDocument).getMaximumFractionDigits();
	}

	/**
	 * @param maximumFractionDigits the maximum number of fraction digits this field shows
	 * @throws IllegalStateException in case this NumberField is not based on a decimal type
	 */
	public void setMaximumFractionDigits(int maximumFractionDigits) {
		NumberDocument<T> typedDocument = document();
		if (!(typedDocument instanceof DecimalDocument)) {
			throw new IllegalStateException("This is not a decimal based NumberField");
		}

		((DecimalDocument<Double>) document()).setMaximumFractionDigits(maximumFractionDigits);
	}

	/**
	 * Specifies whether this number field should convert a grouping separator symbol
	 * to a decimal separator symbol when typed. This solves the problem of locale
	 * controlling whether the numpad comma acts as a decimal symbol, which is usually what we want.
	 * True by default.
	 * @param convertGroupingToDecimalSeparator true if grouping separators should be converted to decimal separators when typed
	 */
	public void setConvertGroupingToDecimalSeparator(boolean convertGroupingToDecimalSeparator) {
		document().getDocumentFilter().setConvertGroupingToDecimalSeparator(convertGroupingToDecimalSeparator);
	}

	/**
	 * @return true if grouping separators should be converted to decimal separators when typed
	 */
	public boolean isConvertGroupingToDecimalSeparator() {
		return document().getDocumentFilter().isConvertGroupingToDecimalSeparator();
	}

	/**
	 * Default false.
	 * @param silentValidation true if invalid input should be silently prevented instead of throwing validation exceptions
	 */
	public void setSilentValidation(boolean silentValidation) {
		document().getDocumentFilter().setSilentValidation(silentValidation);
	}

	/**
	 * @return true if invalid input should be silently prevented instead of throwing validation exceptions
	 */
	public boolean isSilentValidation() {
		return document().getDocumentFilter().isSilentValidation();
	}

	/**
	 * @return a {@link Mutable} instance for the underlying number
	 */
	public Mutable<T> number() {
		return document().number();
	}

	/**
	 * @param valueClass the value class
	 * @param <T> the value type
	 * @return a builder for a component
	 */
	public static <T extends Number> Builder<T> builder(Class<T> valueClass) {
		return createBuilder(valueClass, null);
	}

	/**
	 * @param valueClass the value class
	 * @param linkedValue the value to link to the component
	 * @param <T> the value type
	 * @return a builder for a component
	 */
	public static <T extends Number> Builder<T> builder(Class<T> valueClass, Value<T> linkedValue) {
		return createBuilder(valueClass, requireNonNull(linkedValue));
	}

	/**
	 * Can't override getDocument() with type cast since it's called before setting the document with a class cast exception.
	 * @return the typed document.
	 */
	NumberDocument<T> document() {
		return (NumberDocument<T>) super.getDocument();
	}

	private static <T extends Number> Builder<T> createBuilder(Class<T> valueClass, Value<T> linkedValue) {
		requireNonNull(valueClass);
		if (valueClass.equals(Short.class)) {
			return (Builder<T>) new DefaultShortFieldBuilder((Value<Short>) linkedValue);
		}
		if (valueClass.equals(Integer.class)) {
			return (Builder<T>) new DefaultIntegerFieldBuilder((Value<Integer>) linkedValue);
		}
		if (valueClass.equals(Long.class)) {
			return (Builder<T>) new DefaultLongFieldBuilder((Value<Long>) linkedValue);
		}
		if (valueClass.equals(Double.class)) {
			return (Builder<T>) new DefaultDoubleFieldBuilder((Value<Double>) linkedValue);
		}
		if (valueClass.equals(BigDecimal.class)) {
			return (Builder<T>) new DefaultBigDecimalFieldBuilder((Value<BigDecimal>) linkedValue);
		}

		throw new IllegalArgumentException("Unsupported number type: " + valueClass);
	}

	/**
	 * Builds a NumberField
	 * @param <T> the value type
	 */
	public interface Builder<T extends Number> extends TextFieldBuilder<T, NumberField<T>, Builder<T>> {

		/**
		 * Specifies whether the {@link ComponentValue} created by this builder is nullable, default true.
		 * Note that setting this to false does not prevent the field from containing no value.
		 * @param nullable if false then the {@link ComponentValue} returns 0 when the field contains no value
		 * @return this builder instance
		 */
		Builder<T> nullable(boolean nullable);

		/**
		 * @param minimumValue the minimum value
		 * @param maximumValue the maximum value
		 * @return this builder instance
		 */
		Builder<T> valueRange(Number minimumValue, Number maximumValue);

		/**
		 * @param minimumValue the minimum numerical value
		 * @return this builder instance
		 */
		Builder<T> minimumValue(Number minimumValue);

		/**
		 * @param maximumValue the maximum numerical value
		 * @return this builder instance
		 */
		Builder<T> maximumValue(Number maximumValue);

		/**
		 * @param silentValidation true if invalid input should be silently prevented instead of throwing validation exceptions
		 * @return this builder instance
		 */
		Builder<T> silentValidation(boolean silentValidation);

		/**
		 * @param groupingSeparator the grouping separator
		 * @return this builder instance
		 */
		Builder<T> groupingSeparator(char groupingSeparator);

		/**
		 * Note that this is overridden by {@link #format(java.text.Format)}.
		 * @param groupingUsed true if grouping should be used
		 * @return this builder instance
		 */
		Builder<T> groupingUsed(boolean groupingUsed);

		/**
		 * @param maximumFractionDigits the maximum fraction digits
		 * @return this builder instance
		 */
		Builder<T> maximumFractionDigits(int maximumFractionDigits);

		/**
		 * Set the decimal separator for this field
		 * @param decimalSeparator the decimal separator
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the decimal separator is the same as the grouping separator
		 */
		Builder<T> decimalSeparator(char decimalSeparator);

		/**
		 * Specifies whether the number field should convert a grouping separator symbol
		 * to a decimal separator symbol when typed. This solves the problem of locale
		 * controlling whether the numpad comma acts as a decimal symbol, which is usually what we want.
		 * @param convertGroupingToDecimalSeparator true if grouping separators should be converted to decimal separators when typed
		 * @return this builder instance
		 * @see #CONVERT_GROUPING_TO_DECIMAL_SEPARATOR
		 */
		Builder<T> convertGroupingToDecimalSeparator(boolean convertGroupingToDecimalSeparator);
	}

	private final class GroupingSkipAdapter extends KeyAdapter {
		@Override
		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
				case VK_BACK_SPACE:
					skipGroupingSeparator(false);
					break;
				case VK_DELETE:
					skipGroupingSeparator(true);
					break;
				default:
					break;
			}
		}

		private void skipGroupingSeparator(boolean forward) {
			NumberDocument<?> numberDocument = document();
			char groupingSeparator = ((DecimalFormat) numberDocument.getFormat()).getDecimalFormatSymbols().getGroupingSeparator();
			try {
				int caretPosition = getCaretPosition();
				if (forward && caretPosition < getDocument().getLength() - 1) {
					char afterCaret = numberDocument.getText(caretPosition, 1).charAt(0);
					if (groupingSeparator == afterCaret) {
						setCaretPosition(caretPosition + 1);
					}
				}
				else if (!forward && caretPosition > 0) {
					char beforeCaret = numberDocument.getText(caretPosition - 1, 1).charAt(0);
					if (groupingSeparator == beforeCaret) {
						setCaretPosition(caretPosition - 1);
					}
				}
			}
			catch (BadLocationException ignored) {/*Not happening*/}
		}
	}

	private abstract static class AbstractNumberFieldBuilder<T extends Number>
					extends DefaultTextFieldBuilder<T, NumberField<T>, Builder<T>> implements Builder<T> {

		protected boolean nullable = true;

		private Number maximumValue;
		private Number minimumValue;
		private boolean silentValidation = false;
		private char groupingSeparator = 0;
		private Boolean groupingUsed;
		private char decimalSeparator = 0;
		private int maximumFractionDigits = -1;
		private boolean convertGroupingToDecimalSeparator = CONVERT_GROUPING_TO_DECIMAL_SEPARATOR.get();

		protected AbstractNumberFieldBuilder(Class<T> type, Value<T> linkedValue) {
			super(type, linkedValue);
		}

		@Override
		public Builder<T> nullable(boolean nullable) {
			this.nullable = nullable;
			return this;
		}

		@Override
		public final Builder<T> valueRange(Number minimumValue, Number maximumValue) {
			minimumValue(minimumValue);
			maximumValue(maximumValue);
			return this;
		}

		@Override
		public final Builder<T> minimumValue(Number minimumValue) {
			this.minimumValue = minimumValue;
			return this;
		}

		@Override
		public final Builder<T> maximumValue(Number maximumValue) {
			this.maximumValue = maximumValue;
			return this;
		}

		@Override
		public Builder<T> silentValidation(boolean silentValidation) {
			this.silentValidation = silentValidation;
			return this;
		}

		@Override
		public final Builder<T> groupingSeparator(char groupingSeparator) {
			this.groupingSeparator = groupingSeparator;
			return this;
		}

		@Override
		public final Builder<T> groupingUsed(boolean groupingUsed) {
			this.groupingUsed = groupingUsed;
			return this;
		}

		@Override
		public final Builder<T> maximumFractionDigits(int maximumFractionDigits) {
			this.maximumFractionDigits = maximumFractionDigits;
			return this;
		}

		@Override
		public final Builder<T> decimalSeparator(char decimalSeparator) {
			if (decimalSeparator == groupingSeparator) {
				throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
			}
			this.decimalSeparator = decimalSeparator;
			return this;
		}

		@Override
		public final Builder<T> convertGroupingToDecimalSeparator(boolean convertGroupingToDecimalSeparator) {
			this.convertGroupingToDecimalSeparator = convertGroupingToDecimalSeparator;
			return this;
		}

		@Override
		protected final NumberField<T> createTextField() {
			return new NumberField<>(this);
		}

		protected abstract NumberDocument<T> createDocument();

		protected final NumberFormat initializeFormat(NumberFormat defaultFormat) {
			NumberFormat format = (NumberFormat) format();
			if (format != null) {
				NumberFormat cloned = (NumberFormat) format.clone();
				cloned.setGroupingUsed(format.isGroupingUsed());
				cloned.setMaximumIntegerDigits(format.getMaximumIntegerDigits());
				cloned.setMaximumFractionDigits(format.getMaximumFractionDigits());
				cloned.setMinimumFractionDigits(format.getMinimumFractionDigits());
				cloned.setRoundingMode(format.getRoundingMode());
				cloned.setCurrency(format.getCurrency());
				cloned.setParseIntegerOnly(format.isParseIntegerOnly());

				return cloned;
			}

			return defaultFormat;
		}
	}

	private static NumberFormat nonGroupingIntegerFormat() {
		NumberFormat format = NumberFormat.getIntegerInstance();
		format.setGroupingUsed(false);

		return format;
	}

	private static DecimalFormat decimalFormat() {
		DecimalFormat decimalFormat = new DecimalFormat();
		decimalFormat.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);

		return decimalFormat;
	}

	private static final class DefaultBigDecimalFieldBuilder extends AbstractNumberFieldBuilder<BigDecimal> {

		private DefaultBigDecimalFieldBuilder(Value<BigDecimal> linkedValue) {
			super(BigDecimal.class, linkedValue);
		}

		@Override
		protected NumberDocument<BigDecimal> createDocument() {
			return new DecimalDocument<>((DecimalFormat) initializeFormat(decimalFormat()), true);
		}

		@Override
		protected ComponentValue<BigDecimal, NumberField<BigDecimal>> createComponentValue(NumberField<BigDecimal> component) {
			return new BigDecimalFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultDoubleFieldBuilder extends AbstractNumberFieldBuilder<Double> {

		private DefaultDoubleFieldBuilder(Value<Double> linkedValue) {
			super(Double.class, linkedValue);
		}

		@Override
		protected NumberDocument<Double> createDocument() {
			return new DecimalDocument<>((DecimalFormat) initializeFormat(decimalFormat()), false);
		}

		@Override
		protected ComponentValue<Double, NumberField<Double>> createComponentValue(NumberField<Double> component) {
			return new DoubleFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultShortFieldBuilder extends AbstractNumberFieldBuilder<Short> {

		private DefaultShortFieldBuilder(Value<Short> linkedValue) {
			super(Short.class, linkedValue);
		}

		@Override
		protected NumberDocument<Short> createDocument() {
			return new NumberDocument<>(initializeFormat(nonGroupingIntegerFormat()), Short.class);
		}

		@Override
		protected ComponentValue<Short, NumberField<Short>> createComponentValue(NumberField<Short> component) {
			return new ShortFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultIntegerFieldBuilder extends AbstractNumberFieldBuilder<Integer> {

		private DefaultIntegerFieldBuilder(Value<Integer> linkedValue) {
			super(Integer.class, linkedValue);
		}

		@Override
		protected NumberDocument<Integer> createDocument() {
			return new NumberDocument<>(initializeFormat(nonGroupingIntegerFormat()), Integer.class);
		}

		@Override
		protected ComponentValue<Integer, NumberField<Integer>> createComponentValue(NumberField<Integer> component) {
			return new IntegerFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultLongFieldBuilder extends AbstractNumberFieldBuilder<Long> {

		private DefaultLongFieldBuilder(Value<Long> linkedValue) {
			super(Long.class, linkedValue);
		}

		@Override
		protected NumberDocument<Long> createDocument() {
			return new NumberDocument<>(initializeFormat(nonGroupingIntegerFormat()), Long.class);
		}

		@Override
		protected ComponentValue<Long, NumberField<Long>> createComponentValue(NumberField<Long> component) {
			return new LongFieldValue(component, nullable, updateOn());
		}
	}

	private static final class ShortFieldValue extends AbstractTextComponentValue<Short, NumberField<Short>> {

		private ShortFieldValue(NumberField<Short> shortField, boolean nullable, UpdateOn updateOn) {
			super(shortField, nullable ? null : (short) 0, updateOn);
		}

		@Override
		protected Short getComponentValue() {
			Number number = component().number().get();
			if (number == null) {
				return nullable() ? null : (short) 0;
			}

			return number.shortValue();
		}

		@Override
		protected void setComponentValue(Short value) {
			component().number().set(value);
		}
	}

	private static final class DoubleFieldValue extends AbstractTextComponentValue<Double, NumberField<Double>> {

		private DoubleFieldValue(NumberField<Double> doubleField, boolean nullable, UpdateOn updateOn) {
			super(doubleField, nullable ? null : 0d, updateOn);
		}

		@Override
		protected Double getComponentValue() {
			Number number = component().number().get();
			if (number == null) {
				return nullable() ? null : 0d;
			}

			return number.doubleValue();
		}

		@Override
		protected void setComponentValue(Double value) {
			component().number().set(value);
		}
	}

	private static final class IntegerFieldValue extends AbstractTextComponentValue<Integer, NumberField<Integer>> {

		private IntegerFieldValue(NumberField<Integer> integerField, boolean nullable, UpdateOn updateOn) {
			super(integerField, nullable ? null : 0, updateOn);
		}

		@Override
		protected Integer getComponentValue() {
			Number number = component().number().get();
			if (number == null) {
				return nullable() ? null : 0;
			}

			return number.intValue();
		}

		@Override
		protected void setComponentValue(Integer value) {
			component().number().set(value);
		}
	}

	private static final class LongFieldValue extends AbstractTextComponentValue<Long, NumberField<Long>> {

		private LongFieldValue(NumberField<Long> longField, boolean nullable, UpdateOn updateOn) {
			super(longField, nullable ? null : 0L, updateOn);
		}

		@Override
		protected Long getComponentValue() {
			Number number = component().number().get();
			if (number == null) {
				return nullable() ? null : 0L;
			}

			return number.longValue();
		}

		@Override
		protected void setComponentValue(Long value) {
			component().number().set(value);
		}
	}

	private static final class BigDecimalFieldValue extends AbstractTextComponentValue<BigDecimal, NumberField<BigDecimal>> {

		private BigDecimalFieldValue(NumberField<BigDecimal> doubleField, boolean nullable, UpdateOn updateOn) {
			super(doubleField, nullable ? null : BigDecimal.ZERO, updateOn);
		}

		@Override
		protected BigDecimal getComponentValue() {
			BigDecimal number = component().number().get();
			if (number == null) {
				return nullable() ? null : BigDecimal.ZERO;
			}

			return number;
		}

		@Override
		protected void setComponentValue(BigDecimal value) {
			component().number().set(value);
		}
	}
}
