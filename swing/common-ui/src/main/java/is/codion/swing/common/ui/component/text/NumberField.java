/*
 * Copyright (c) 2015 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.text.NumberDocument.DecimalDocument;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.function.Consumer;

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
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> CONVERT_GROUPING_TO_DECIMAL_SEPARATOR =
          Configuration.booleanValue("is.codion.swing.common.ui.component.text.NumberField.convertGroupingToDecimalSeparator", true);

  private final Value<T> value = Value.value();

  private NumberField(NumberDocument<T> document) {
    super(document);
    document.setTextComponent(this);
    if (document.getFormat() instanceof DecimalFormat) {
      addKeyListener(new GroupingSkipAdapter());
    }
    document.addDocumentListener(new SetNumberListener());
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
    getTypedDocument().setGroupingUsed(groupingUsed);
  }

  /**
   * @param number the number to display in this field
   */
  public void setNumber(T number) {
    getTypedDocument().setNumber(number);
  }

  /**
   * @return the number being displayed in this field
   */
  public T getNumber() {
    return getTypedDocument().getNumber();
  }

  /**
   * Sets the range of values this field should allow
   * @param minimumValue the minimum value
   * @param maximumValue the maximum value
   */
  public void setValueRange(Number minimumValue, Number maximumValue) {
    getTypedDocument().getDocumentFilter().setMinimumValue(minimumValue);
    getTypedDocument().getDocumentFilter().setMaximumValue(maximumValue);
  }

  /**
   * @param minimumValue the minimum value
   */
  public void setMinimumValue(Number minimumValue) {
    getTypedDocument().getDocumentFilter().setMinimumValue(minimumValue);
  }

  /**
   * @return the minimum value this field should accept
   */
  public Number getMinimumValue() {
    return getTypedDocument().getDocumentFilter().getMinimumValue();
  }

  /**
   * @param maximumValue the maximum value
   */
  public void setMaximumValue(Number maximumValue) {
    getTypedDocument().getDocumentFilter().setMaximumValue(maximumValue);
  }

  /**
   * @return the maximum value this field should accept
   */
  public Number getMaximumValue() {
    return getTypedDocument().getDocumentFilter().getMaximumValue();
  }

  /**
   * Set the decimal and grouping separators for this field
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping separator
   * @throws IllegalArgumentException in case both separators are the same character
   */
  public void setSeparators(char decimalSeparator, char groupingSeparator) {
    getTypedDocument().setSeparators(decimalSeparator, groupingSeparator);
  }

  /**
   * Sets the decimal separator
   * @param decimalSeparator the separator
   */
  public void setDecimalSeparator(char decimalSeparator) {
    getTypedDocument().setDecimalSeparator(decimalSeparator);
  }

  /**
   * Sets the grouping separator
   * @param groupingSeparator the separator
   */
  public void setGroupingSeparator(char groupingSeparator) {
    getTypedDocument().setGroupingSeparator(groupingSeparator);
  }

  /**
   * @return the maximum number of fraction digits this field shows
   * @throws IllegalStateException in case this NumberField is not based on a decimal type
   */
  public int getMaximumFractionDigits() {
    NumberDocument<T> typedDocument = getTypedDocument();
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
    NumberDocument<T> typedDocument = getTypedDocument();
    if (!(typedDocument instanceof DecimalDocument)) {
      throw new IllegalStateException("This is not a decimal based NumberField");
    }

    ((DecimalDocument<Double>) getTypedDocument()).setMaximumFractionDigits(maximumFractionDigits);
  }

  /**
   * Specifies whether this number field should convert a grouping separator symbol
   * to a decimal separator symbol when typed. This solves the problem of locale
   * controlling whether the numpad comma acts as a decimal symbol, which is usually what we want.
   * True by default.
   * @param convertGroupingToDecimalSeparator true if grouping separators should be converted to decimal separators when typed
   */
  public void setConvertGroupingToDecimalSeparator(boolean convertGroupingToDecimalSeparator) {
    getTypedDocument().getDocumentFilter().setConvertGroupingToDecimalSeparator(convertGroupingToDecimalSeparator);
  }

  /**
   * @return true if grouping separators should be converted to decimal separators when typed
   */
  public boolean isConvertGroupingToDecimalSeparator() {
    return getTypedDocument().getDocumentFilter().isConvertGroupingToDecimalSeparator();
  }

  /**
   * @param listener a listener notified when the value changes
   */
  public void addValueListener(Consumer<T> listener) {
    value.addDataListener(listener);
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
  NumberDocument<T> getTypedDocument() {
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
     * True by default.
     * @param convertGroupingToDecimalSeparator true if grouping separators should be converted to decimal separators when typed
     * @return this builder instance
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
      NumberDocument<?> numberDocument = getTypedDocument();
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

  private final class SetNumberListener implements DocumentAdapter {
    @Override
    public void contentsChanged(DocumentEvent e) {
      value.set(getTypedDocument().getNumber());
    }
  }

  private abstract static class AbstractNumberFieldBuilder<T extends Number>
          extends DefaultTextFieldBuilder<T, NumberField<T>, Builder<T>> implements Builder<T> {

    protected boolean nullable = true;

    private Number maximumValue;
    private Number minimumValue;
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
      NumberField<T> numberField = createNumberField(initializeFormat());
      numberField.setMinimumValue(minimumValue);
      numberField.setMaximumValue(maximumValue);
      numberField.setConvertGroupingToDecimalSeparator(convertGroupingToDecimalSeparator);
      if (groupingUsed != null) {
        numberField.setGroupingUsed(groupingUsed);
      }
      if (groupingSeparator != 0) {
        numberField.setGroupingSeparator(groupingSeparator);
      }
      if (numberField.getDocument() instanceof DecimalDocument) {
        if (maximumFractionDigits != -1) {
          numberField.setMaximumFractionDigits(maximumFractionDigits);
        }
        if (decimalSeparator != 0) {
          numberField.setDecimalSeparator(decimalSeparator);
        }
      }

      return numberField;
    }

    protected abstract NumberField<T> createNumberField(NumberFormat format);

    protected abstract NumberFormat createFormat();

    @Override
    protected final void setInitialValue(NumberField<T> component, T initialValue) {
      component.setNumber(initialValue);
    }

    private NumberFormat initializeFormat() {
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

      return createFormat();
    }
  }

  private static NumberFormat nonGroupingIntegerFormat() {
    NumberFormat format = NumberFormat.getIntegerInstance();
    format.setGroupingUsed(false);

    return format;
  }

  private static final class DefaultBigDecimalFieldBuilder extends AbstractNumberFieldBuilder<BigDecimal> {

    private DefaultBigDecimalFieldBuilder(Value<BigDecimal> linkedValue) {
      super(BigDecimal.class, linkedValue);
    }

    @Override
    protected NumberField<BigDecimal> createNumberField(NumberFormat format) {
      return new NumberField<>(new DecimalDocument<>((DecimalFormat) format, true));
    }

    @Override
    protected ComponentValue<BigDecimal, NumberField<BigDecimal>> createComponentValue(NumberField<BigDecimal> component) {
      return new BigDecimalFieldValue(component, nullable, updateOn);
    }

    @Override
    protected NumberFormat createFormat() {
      DecimalFormat decimalFormat = new DecimalFormat();
      decimalFormat.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);

      return decimalFormat;
    }
  }

  private static final class DefaultDoubleFieldBuilder extends AbstractNumberFieldBuilder<Double> {

    private DefaultDoubleFieldBuilder(Value<Double> linkedValue) {
      super(Double.class, linkedValue);
    }

    @Override
    protected NumberField<Double> createNumberField(NumberFormat format) {
      return new NumberField<>(new DecimalDocument<>((DecimalFormat) format, false));
    }

    @Override
    protected ComponentValue<Double, NumberField<Double>> createComponentValue(NumberField<Double> component) {
      return new DoubleFieldValue(component, nullable, updateOn);
    }

    @Override
    protected NumberFormat createFormat() {
      DecimalFormat decimalFormat = new DecimalFormat();
      decimalFormat.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);

      return decimalFormat;
    }
  }

  private static final class DefaultShortFieldBuilder extends AbstractNumberFieldBuilder<Short> {

    private DefaultShortFieldBuilder(Value<Short> linkedValue) {
      super(Short.class, linkedValue);
    }

    @Override
    protected NumberField<Short> createNumberField(NumberFormat format) {
      return new NumberField<>(new NumberDocument<>(format, Short.class));
    }

    @Override
    protected ComponentValue<Short, NumberField<Short>> createComponentValue(NumberField<Short> component) {
      return new ShortFieldValue(component, nullable, updateOn);
    }

    @Override
    protected NumberFormat createFormat() {
      return nonGroupingIntegerFormat();
    }
  }

  private static final class DefaultIntegerFieldBuilder extends AbstractNumberFieldBuilder<Integer> {

    private DefaultIntegerFieldBuilder(Value<Integer> linkedValue) {
      super(Integer.class, linkedValue);
    }

    @Override
    protected NumberField<Integer> createNumberField(NumberFormat format) {
      return new NumberField<>(new NumberDocument<>(format, Integer.class));
    }

    @Override
    protected ComponentValue<Integer, NumberField<Integer>> createComponentValue(NumberField<Integer> component) {
      return new IntegerFieldValue(component, nullable, updateOn);
    }

    @Override
    protected NumberFormat createFormat() {
      return nonGroupingIntegerFormat();
    }
  }

  private static final class DefaultLongFieldBuilder extends AbstractNumberFieldBuilder<Long> {

    private DefaultLongFieldBuilder(Value<Long> linkedValue) {
      super(Long.class, linkedValue);
    }

    @Override
    protected NumberField<Long> createNumberField(NumberFormat format) {
      return new NumberField<>(new NumberDocument<>(format, Long.class));
    }

    @Override
    protected ComponentValue<Long, NumberField<Long>> createComponentValue(NumberField<Long> component) {
      return new LongFieldValue(component, nullable, updateOn);
    }

    @Override
    protected NumberFormat createFormat() {
      return nonGroupingIntegerFormat();
    }
  }

  private static final class ShortFieldValue extends AbstractTextComponentValue<Short, NumberField<Short>> {

    private ShortFieldValue(NumberField<Short> shortField, boolean nullable, UpdateOn updateOn) {
      super(shortField, nullable ? null : (short) 0, updateOn);
    }

    @Override
    protected Short getComponentValue() {
      Number number = component().getNumber();
      if (number == null) {
        return nullable() ? null : (short) 0;
      }

      return number.shortValue();
    }

    @Override
    protected void setComponentValue(Short value) {
      component().setNumber(value);
    }
  }

  private static final class DoubleFieldValue extends AbstractTextComponentValue<Double, NumberField<Double>> {

    private DoubleFieldValue(NumberField<Double> doubleField, boolean nullable, UpdateOn updateOn) {
      super(doubleField, nullable ? null : 0d, updateOn);
    }

    @Override
    protected Double getComponentValue() {
      Number number = component().getNumber();
      if (number == null) {
        return nullable() ? null : 0d;
      }

      return number.doubleValue();
    }

    @Override
    protected void setComponentValue(Double value) {
      component().setNumber(value);
    }
  }

  private static final class IntegerFieldValue extends AbstractTextComponentValue<Integer, NumberField<Integer>> {

    private IntegerFieldValue(NumberField<Integer> integerField, boolean nullable, UpdateOn updateOn) {
      super(integerField, nullable ? null : 0, updateOn);
    }

    @Override
    protected Integer getComponentValue() {
      Number number = component().getNumber();
      if (number == null) {
        return nullable() ? null : 0;
      }

      return number.intValue();
    }

    @Override
    protected void setComponentValue(Integer value) {
      component().setNumber(value);
    }
  }

  private static final class LongFieldValue extends AbstractTextComponentValue<Long, NumberField<Long>> {

    private LongFieldValue(NumberField<Long> longField, boolean nullable, UpdateOn updateOn) {
      super(longField, nullable ? null : 0L, updateOn);
    }

    @Override
    protected Long getComponentValue() {
      Number number = component().getNumber();
      if (number == null) {
        return nullable() ? null : 0L;
      }

      return number.longValue();
    }

    @Override
    protected void setComponentValue(Long value) {
      component().setNumber(value);
    }
  }

  private static final class BigDecimalFieldValue extends AbstractTextComponentValue<BigDecimal, NumberField<BigDecimal>> {

    private BigDecimalFieldValue(NumberField<BigDecimal> doubleField, boolean nullable, UpdateOn updateOn) {
      super(doubleField, nullable ? null : BigDecimal.ZERO, updateOn);
    }

    @Override
    protected BigDecimal getComponentValue() {
      BigDecimal number = component().getNumber();
      if (number == null) {
        return nullable() ? null : BigDecimal.ZERO;
      }

      return number;
    }

    @Override
    protected void setComponentValue(BigDecimal value) {
      component().setNumber(value);
    }
  }
}
