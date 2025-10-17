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
 * Copyright (c) 2015 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.observer.Observable;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.component.text.NumberDocument.DecimalDocument;
import is.codion.swing.common.ui.component.text.NumberDocument.NumberParsingDocumentFilter;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Optional;

import static is.codion.common.Configuration.booleanValue;
import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.util.Objects.requireNonNull;

/**
 * A text field for numbers.
 * Use {@link #builder()} for {@link Builder} instances.
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
					booleanValue(NumberField.class.getName() + ".convertGroupingToDecimalSeparator", true);

	private NumberField(AbstractNumberFieldBuilder<T> builder) {
		super(builder.createDocument());
		NumberDocument<T> document = document();
		document.setTextComponent(this);
		NumberParsingDocumentFilter<T> documentFilter = document.getDocumentFilter();
		documentFilter.setMinimumValue(builder.minimum);
		documentFilter.setMaximumValue(builder.maximum);
		documentFilter.setSilentValidation(builder.silentValidation);
		documentFilter.setConvertGroupingToDecimalSeparator(builder.convertGroupingToDecimalSeparator);
		if (document.format() instanceof DecimalFormat) {
			addKeyListener(new GroupingSkipAdapter());
		}
		if (builder.grouping != null) {
			document.setGroupingUsed(builder.grouping);
		}
		if (builder.groupingSeparator != 0) {
			document.setGroupingSeparator(builder.groupingSeparator);
		}
		if (document instanceof DecimalDocument) {
			if (builder.fractionDigits != -1) {
				((DecimalDocument<?>) document).setMaximumFractionDigits(builder.fractionDigits);
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
	 * @return the value currently being displayed or an empty Optional in case of null
	 */
	public Optional<T> optional() {
		return document().observable().optional();
	}

	/**
	 * @param number the number to set
	 */
	public void set(@Nullable T number) {
		document().set(number);
	}

	/**
	 * @return the number
	 */
	public @Nullable T get() {
		return document().get();
	}

	/**
	 * @return an {@link Observable} based on the number
	 */
	public Observable<T> observable() {
		return document().observable();
	}

	/**
	 * @return a {@link Builder.NumberClassStep}
	 */
	public static Builder.NumberClassStep builder() {
		return AbstractNumberFieldBuilder.VALUE_CLASS;
	}

	/**
	 * Can't override getDocument() with type cast since it's called before setting the document with a class cast exception.
	 * @return the typed document.
	 */
	NumberDocument<T> document() {
		return (NumberDocument<T>) super.getDocument();
	}

	/**
	 * Builds a NumberField
	 * @param <T> the value type
	 */
	public interface Builder<T extends Number> extends TextFieldBuilder<NumberField<T>, T, Builder<T>> {

		/**
		 * Provides a {@link Builder}
		 */
		interface NumberClassStep {

			/**
			 * @param numberClass the number class
			 * @param <T> the value type
			 * @return a {@link Builder}
			 */
			<T extends Number> Builder<T> numberClass(Class<T> numberClass);
		}

		/**
		 * Specifies whether the {@link ComponentValue} created by this builder is nullable, default true.
		 * Note that setting this to false does not prevent the field from containing no value.
		 * @param nullable if false then the {@link ComponentValue} returns 0 when the field contains no value
		 * @return this builder instance
		 */
		Builder<T> nullable(boolean nullable);

		/**
		 * @param minimum the minimum value
		 * @param maximum the maximum value
		 * @return this builder instance
		 */
		Builder<T> range(@Nullable Number minimum, @Nullable Number maximum);

		/**
		 * @param minimum the minimum numerical value
		 * @return this builder instance
		 */
		Builder<T> minimum(@Nullable Number minimum);

		/**
		 * @param maximum the maximum numerical value
		 * @return this builder instance
		 */
		Builder<T> maximum(@Nullable Number maximum);

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
		 * @param grouping true if grouping should be used
		 * @return this builder instance
		 * @see NumberFormat#setGroupingUsed(boolean)
		 */
		Builder<T> grouping(boolean grouping);

		/**
		 * @param fractionDigits the maximum fraction digits
		 * @return this builder instance
		 */
		Builder<T> fractionDigits(int fractionDigits);

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
			char groupingSeparator = ((DecimalFormat) numberDocument.format()).getDecimalFormatSymbols().getGroupingSeparator();
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

	private static final class DefaultNumberClassStep implements Builder.NumberClassStep {

		@Override
		public <T extends Number> Builder<T> numberClass(Class<T> numberClass) {
			requireNonNull(numberClass);
			if (numberClass.equals(Short.class)) {
				return (Builder<T>) new DefaultShortFieldBuilder();
			}
			if (numberClass.equals(Integer.class)) {
				return (Builder<T>) new DefaultIntegerFieldBuilder();
			}
			if (numberClass.equals(Long.class)) {
				return (Builder<T>) new DefaultLongFieldBuilder();
			}
			if (numberClass.equals(Double.class)) {
				return (Builder<T>) new DefaultDoubleFieldBuilder();
			}
			if (numberClass.equals(BigDecimal.class)) {
				return (Builder<T>) new DefaultBigDecimalFieldBuilder();
			}

			throw new IllegalArgumentException("Unsupported number type: " + numberClass);
		}
	}

	private abstract static class AbstractNumberFieldBuilder<T extends Number>
					extends DefaultTextFieldBuilder<NumberField<T>, T, Builder<T>> implements Builder<T> {

		private static final Builder.NumberClassStep VALUE_CLASS = new DefaultNumberClassStep();

		protected boolean nullable = true;

		private @Nullable Number maximum;
		private @Nullable Number minimum;
		private boolean silentValidation = false;
		private char groupingSeparator = 0;
		private @Nullable Boolean grouping;
		private char decimalSeparator = 0;
		private int fractionDigits = -1;
		private boolean convertGroupingToDecimalSeparator = CONVERT_GROUPING_TO_DECIMAL_SEPARATOR.getOrThrow();

		protected AbstractNumberFieldBuilder(Class<T> type) {
			super(type);
		}

		@Override
		public final Builder<T> maximumLength(int maximumLength) {
			throw new UnsupportedOperationException("NumberField uses numerical ranges, not text length to restrict its input");
		}

		@Override
		public final Builder<T> nullable(boolean nullable) {
			this.nullable = nullable;
			return this;
		}

		@Override
		public final Builder<T> range(@Nullable Number minimum, @Nullable Number maximum) {
			minimum(minimum);
			maximum(maximum);
			return this;
		}

		@Override
		public final Builder<T> minimum(@Nullable Number minimum) {
			if (maximum != null &&  minimum != null && minimum.doubleValue() > maximum.doubleValue()) {
				throw new IllegalArgumentException("minimum can't be greater than maximum");
			}
			this.minimum = minimum;
			return this;
		}

		@Override
		public final Builder<T> maximum(@Nullable Number maximum) {
			if (maximum != null &&  minimum != null && maximum.doubleValue() < minimum.doubleValue()) {
				throw new IllegalArgumentException("maximum can't be greater than minimum");
			}
			this.maximum = maximum;
			return this;
		}

		@Override
		public final Builder<T> silentValidation(boolean silentValidation) {
			this.silentValidation = silentValidation;
			return this;
		}

		@Override
		public final Builder<T> groupingSeparator(char groupingSeparator) {
			if (groupingSeparator == decimalSeparator) {
				throw new IllegalArgumentException("Grouping separator must not be the same as decimal separator");
			}
			this.groupingSeparator = groupingSeparator;
			return this;
		}

		@Override
		public final Builder<T> grouping(boolean grouping) {
			this.grouping = grouping;
			return this;
		}

		@Override
		public final Builder<T> fractionDigits(int fractionDigits) {
			this.fractionDigits = fractionDigits;
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

		private DefaultBigDecimalFieldBuilder() {
			super(BigDecimal.class);
		}

		@Override
		protected NumberDocument<BigDecimal> createDocument() {
			return new DecimalDocument<>((DecimalFormat) initializeFormat(decimalFormat()), true);
		}

		@Override
		protected ComponentValue<NumberField<BigDecimal>, BigDecimal> createComponentValue(NumberField<BigDecimal> component) {
			return new BigDecimalFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultDoubleFieldBuilder extends AbstractNumberFieldBuilder<Double> {

		private DefaultDoubleFieldBuilder() {
			super(Double.class);
		}

		@Override
		protected NumberDocument<Double> createDocument() {
			return new DecimalDocument<>((DecimalFormat) initializeFormat(decimalFormat()), false);
		}

		@Override
		protected ComponentValue<NumberField<Double>, Double> createComponentValue(NumberField<Double> component) {
			return new DoubleFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultShortFieldBuilder extends AbstractNumberFieldBuilder<Short> {

		private DefaultShortFieldBuilder() {
			super(Short.class);
		}

		@Override
		protected NumberDocument<Short> createDocument() {
			return new NumberDocument<>(initializeFormat(nonGroupingIntegerFormat()), Short.class);
		}

		@Override
		protected ComponentValue<NumberField<Short>, Short> createComponentValue(NumberField<Short> component) {
			return new ShortFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultIntegerFieldBuilder extends AbstractNumberFieldBuilder<Integer> {

		private DefaultIntegerFieldBuilder() {
			super(Integer.class);
		}

		@Override
		protected NumberDocument<Integer> createDocument() {
			return new NumberDocument<>(initializeFormat(nonGroupingIntegerFormat()), Integer.class);
		}

		@Override
		protected ComponentValue<NumberField<Integer>, Integer> createComponentValue(NumberField<Integer> component) {
			return new IntegerFieldValue(component, nullable, updateOn());
		}
	}

	private static final class DefaultLongFieldBuilder extends AbstractNumberFieldBuilder<Long> {

		private DefaultLongFieldBuilder() {
			super(Long.class);
		}

		@Override
		protected NumberDocument<Long> createDocument() {
			return new NumberDocument<>(initializeFormat(nonGroupingIntegerFormat()), Long.class);
		}

		@Override
		protected ComponentValue<NumberField<Long>, Long> createComponentValue(NumberField<Long> component) {
			return new LongFieldValue(component, nullable, updateOn());
		}
	}

	private static final class ShortFieldValue extends AbstractTextComponentValue<NumberField<Short>, Short> {

		private ShortFieldValue(NumberField<Short> shortField, boolean nullable, UpdateOn updateOn) {
			super(shortField, nullable ? null : (short) 0, updateOn);
		}

		@Override
		protected Short getComponentValue() {
			Number number = component().get();
			if (number == null) {
				return isNullable() ? null : (short) 0;
			}

			return number.shortValue();
		}

		@Override
		protected void setComponentValue(Short value) {
			component().set(value);
		}
	}

	private static final class DoubleFieldValue extends AbstractTextComponentValue<NumberField<Double>, Double> {

		private DoubleFieldValue(NumberField<Double> doubleField, boolean nullable, UpdateOn updateOn) {
			super(doubleField, nullable ? null : 0d, updateOn);
		}

		@Override
		protected Double getComponentValue() {
			Number number = component().get();
			if (number == null) {
				return isNullable() ? null : 0d;
			}

			return number.doubleValue();
		}

		@Override
		protected void setComponentValue(Double value) {
			component().set(value);
		}
	}

	private static final class IntegerFieldValue extends AbstractTextComponentValue<NumberField<Integer>, Integer> {

		private IntegerFieldValue(NumberField<Integer> integerField, boolean nullable, UpdateOn updateOn) {
			super(integerField, nullable ? null : 0, updateOn);
		}

		@Override
		protected Integer getComponentValue() {
			Number number = component().get();
			if (number == null) {
				return isNullable() ? null : 0;
			}

			return number.intValue();
		}

		@Override
		protected void setComponentValue(Integer value) {
			component().set(value);
		}
	}

	private static final class LongFieldValue extends AbstractTextComponentValue<NumberField<Long>, Long> {

		private LongFieldValue(NumberField<Long> longField, boolean nullable, UpdateOn updateOn) {
			super(longField, nullable ? null : 0L, updateOn);
		}

		@Override
		protected Long getComponentValue() {
			Number number = component().get();
			if (number == null) {
				return isNullable() ? null : 0L;
			}

			return number.longValue();
		}

		@Override
		protected void setComponentValue(Long value) {
			component().set(value);
		}
	}

	private static final class BigDecimalFieldValue extends AbstractTextComponentValue<NumberField<BigDecimal>, BigDecimal> {

		private BigDecimalFieldValue(NumberField<BigDecimal> doubleField, boolean nullable, UpdateOn updateOn) {
			super(doubleField, nullable ? null : BigDecimal.ZERO, updateOn);
		}

		@Override
		protected BigDecimal getComponentValue() {
			BigDecimal number = component().get();
			if (number == null) {
				return isNullable() ? null : BigDecimal.ZERO;
			}

			return number;
		}

		@Override
		protected void setComponentValue(BigDecimal value) {
			component().set(value);
		}
	}
}
