/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.event.EventDataListener;
import is.codion.common.formats.Formats;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.textfield.DocumentAdapter;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.textfield.NumberDocument.DecimalDocument;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static java.util.Objects.requireNonNull;

/**
 * A text field for numbers.
 * Use the available factory methods for instances.
 * @param <T> the Number type
 */
public final class NumberField<T extends Number> extends JTextField {

  private final Value<T> value = Value.value();

  NumberField(NumberDocument<T> document) {
    setDocument(document);
    document.setTextComponent(this);
    if (document.getFormat() instanceof DecimalFormat) {
      addKeyListener(new GroupingSkipAdapter());
    }
    document.addDocumentListener((DocumentAdapter) e -> value.set(document.getValue()));
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
    getTypedDocument().getFormat().setGroupingUsed(groupingUsed);
  }

  /**
   * @param value the value to display in this field
   */
  public void setValue(T value) {
    getTypedDocument().setValue(value);
  }

  /**
   * @return the value being displayed in this field
   */
  public T getValue() {
    return getTypedDocument().getValue();
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
   * @param listener a listener notified when the value changes
   */
  public void addValueListener(EventDataListener<T> listener) {
    value.addDataListener(listener);
  }

  /**
   * @param valueClass the value class
   * @param <T> the value type
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <T extends Number, B extends Builder<T, B>> Builder<T, B> builder(Class<T> valueClass) {
    return builder(valueClass, null);
  }

  /**
   * @param valueClass the value class
   * @param linkedValue the value to link to the component
   * @param <T> the value type
   * @param <B> the builder type
   * @return a builder for a component
   */
  public static <T extends Number, B extends Builder<T, B>> Builder<T, B> builder(Class<T> valueClass, Value<T> linkedValue) {
    requireNonNull(valueClass);
    if (valueClass.equals(Integer.class)) {
      return (Builder<T, B>) new DefaultIntegerFieldBuilder<>((Value<Integer>) linkedValue);
    }
    if (valueClass.equals(Long.class)) {
      return (Builder<T, B>) new DefaultLongFieldBuilder<>((Value<Long>) linkedValue);
    }
    if (valueClass.equals(Double.class)) {
      return (Builder<T, B>) new DefaultDoubleFieldBuilder<>((Value<Double>) linkedValue);
    }
    if (valueClass.equals(BigDecimal.class)) {
      return (Builder<T, B>) new DefaultBigDecimalFieldBuilder<>((Value<BigDecimal>) linkedValue);
    }

    throw new IllegalArgumentException("Unsupported number type: " + valueClass);
  }

  /**
   * Can't override getDocument() with type cast since it's called before setting the document with a class cast exception.
   * @return the typed document.
   */
  NumberDocument<T> getTypedDocument() {
    return (NumberDocument<T>) super.getDocument();
  }

  /**
   * Builds a NumberField
   * @param <T> the value type
   * @param <B> the builder type
   */
  public interface Builder<T extends Number, B extends Builder<T, B>> extends TextFieldBuilder<T, NumberField<T>, B> {

    /**
     * @param minimumValue the minimum value
     * @param maximumValue the maximum value
     * @return this builder instance
     */
    B valueRange(Number minimumValue, Number maximumValue);

    /**
     * @param minimumValue the minimum numerical value
     * @return this builder instance
     */
    B minimumValue(Number minimumValue);

    /**
     * @param maximumValue the maximum numerical value
     * @return this builder instance
     */
    B maximumValue(Number maximumValue);

    /**
     * @param groupingSeparator the grouping separator
     * @return this builder instance
     */
    B groupingSeparator(char groupingSeparator);

    /**
     * Note that this is overridden by {@link #format(java.text.Format)}.
     * @param groupingUsed true if grouping should be used
     * @return this builder instance
     */
    B groupingUsed(boolean groupingUsed);
  }

  /**
   * A builder for a decimal based {@link NumberField}.
   */
  public interface DecimalBuilder<T extends Number, B extends DecimalBuilder<T, B>> extends Builder<T, B> {

      /**
     * @param maximumFractionDigits the maximum fraction digits
     * @return this builder instance
     */
    B maximumFractionDigits(int maximumFractionDigits);

    /**
     * Set the decimal separator for this field
     * @param decimalSeparator the decimal separator
     * @return this builder instance
     * @throws IllegalArgumentException in case the decimal separator is the same as the grouping separator
     */
    B decimalSeparator(char decimalSeparator);
  }

  private final class GroupingSkipAdapter extends KeyAdapter {
    @Override
    public void keyReleased(KeyEvent e) {
      switch (e.getKeyCode()) {
        case KeyEvent.VK_BACK_SPACE:
          skipGroupingSeparator(false);
          break;
        case KeyEvent.VK_DELETE:
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

  private abstract static class AbstractNumberFieldBuilder<T extends Number, B extends Builder<T, B>>
          extends DefaultTextFieldBuilder<T, NumberField<T>, B> implements Builder<T, B> {

    private Number maximumValue;
    private Number minimumValue;
    protected char groupingSeparator = 0;
    private boolean groupingUsed;

    protected AbstractNumberFieldBuilder(Class<T> type, Value<T> linkedValue) {
      super(type, linkedValue);
    }

    @Override
    public final B valueRange(Number minimumValue, Number maximumValue) {
      minimumValue(minimumValue);
      maximumValue(maximumValue);
      return (B) this;
    }

    @Override
    public final B minimumValue(Number minimumValue) {
      this.minimumValue = minimumValue;
      return (B) this;
    }

    @Override
    public final B maximumValue(Number maximumValue) {
      this.maximumValue = maximumValue;
      return (B) this;
    }

    @Override
    public final B groupingSeparator(char groupingSeparator) {
      this.groupingSeparator = groupingSeparator;
      return (B) this;
    }

    @Override
    public final B groupingUsed(boolean groupingUsed) {
      this.groupingUsed = groupingUsed;
      return (B) this;
    }

    @Override
    protected final NumberField<T> createTextField() {
      NumberFormat format = cloneFormat((NumberFormat) getFormat());
      NumberField<T> numberField = createNumberField(format);
      numberField.setMinimumValue(minimumValue);
      numberField.setMaximumValue(maximumValue);
      if (groupingSeparator != 0) {
        numberField.setGroupingSeparator(groupingSeparator);
      }
      if (format == null) {
        numberField.setGroupingUsed(groupingUsed);
      }

      return numberField;
    }

    protected abstract NumberField<T> createNumberField(NumberFormat format);

    @Override
    protected final void setInitialValue(NumberField<T> component, T initialValue) {
      component.setValue(initialValue);
    }

    private static NumberFormat cloneFormat(NumberFormat format) {
      if (format == null) {
        return null;
      }
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
  }

  private static final class DefaultBigDecimalFieldBuilder<B extends DecimalBuilder<BigDecimal, B>> extends AbstractNumberFieldBuilder<BigDecimal, B>
          implements DecimalBuilder<BigDecimal, B> {

    private int maximumFractionDigits = -1;
    private char decimalSeparator = 0;

    private DefaultBigDecimalFieldBuilder(Value<BigDecimal> linkedValue) {
      super(BigDecimal.class, linkedValue);
    }

    @Override
    public B maximumFractionDigits(int maximumFractionDigits) {
      this.maximumFractionDigits = maximumFractionDigits;
      return (B) this;
    }

    @Override
    public B decimalSeparator(char decimalSeparator) {
      if (decimalSeparator == groupingSeparator) {
        throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
      }
      this.decimalSeparator = decimalSeparator;
      return (B) this;
    }

    @Override
    protected NumberField<BigDecimal> createNumberField(NumberFormat format) {
      DecimalFormat decimalFormat = (DecimalFormat) format;
      if (decimalFormat == null) {
        decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);
      }
      NumberField<BigDecimal> field = new NumberField<>(new DecimalDocument<>(decimalFormat, true));
      if (decimalSeparator != 0) {
        field.setDecimalSeparator(decimalSeparator);
      }
      if (maximumFractionDigits > 0) {
        field.setMaximumFractionDigits(maximumFractionDigits);
      }

      return field;
    }

    @Override
    protected ComponentValue<BigDecimal, NumberField<BigDecimal>> createComponentValue(NumberField<BigDecimal> component) {
      return new BigDecimalFieldValue(component, true, updateOn);
    }
  }

  private static final class DefaultDoubleFieldBuilder<B extends DecimalBuilder<Double, B>> extends AbstractNumberFieldBuilder<Double, B>
          implements DecimalBuilder<Double, B> {

    private int maximumFractionDigits = -1;
    private char decimalSeparator = 0;

    private DefaultDoubleFieldBuilder(Value<Double> linkedValue) {
      super(Double.class, linkedValue);
    }

    @Override
    public B maximumFractionDigits(int maximumFractionDigits) {
      this.maximumFractionDigits = maximumFractionDigits;
      return (B) this;
    }

    @Override
    public B decimalSeparator(char decimalSeparator) {
      if (decimalSeparator == groupingSeparator) {
        throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
      }
      this.decimalSeparator = decimalSeparator;
      return (B) this;
    }

    @Override
    protected NumberField<Double> createNumberField(NumberFormat format) {
      DecimalFormat decimalFormat = (DecimalFormat) format;
      if (decimalFormat == null) {
        decimalFormat = new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);
      }
      NumberField<Double> field = new NumberField<>(new DecimalDocument<>(decimalFormat, false));
      if (decimalSeparator != 0) {
        field.setDecimalSeparator(decimalSeparator);
      }
      if (maximumFractionDigits > 0) {
        field.setMaximumFractionDigits(maximumFractionDigits);
      }

      return field;
    }

    @Override
    protected ComponentValue<Double, NumberField<Double>> createComponentValue(NumberField<Double> component) {
      return new DoubleFieldValue(component, true, updateOn);
    }
  }

  private static final class DefaultIntegerFieldBuilder<B extends Builder<Integer, B>> extends AbstractNumberFieldBuilder<Integer, B> {

    private DefaultIntegerFieldBuilder(Value<Integer> linkedValue) {
      super(Integer.class, linkedValue);
    }

    @Override
    protected NumberField<Integer> createNumberField(NumberFormat format) {
      if (format == null) {
        format = Formats.getNonGroupingIntegerFormat();
      }

      return new NumberField<>(new NumberDocument<>(format, Integer.class));
    }

    @Override
    protected ComponentValue<Integer, NumberField<Integer>> createComponentValue(NumberField<Integer> component) {
      return new IntegerFieldValue(component, true, updateOn);
    }
  }

  private static final class DefaultLongFieldBuilder<B extends Builder<Long, B>> extends AbstractNumberFieldBuilder<Long, B> {

    private DefaultLongFieldBuilder(Value<Long> linkedValue) {
      super(Long.class, linkedValue);
    }

    @Override
    protected NumberField<Long> createNumberField(NumberFormat format) {
      if (format == null) {
        format = Formats.getNonGroupingIntegerFormat();
      }

      return new NumberField<>(new NumberDocument<>(format, Long.class));
    }

    @Override
    protected ComponentValue<Long, NumberField<Long>> createComponentValue(NumberField<Long> component) {
      return new LongFieldValue(component, true, updateOn);
    }
  }

  private static final class DoubleFieldValue extends AbstractTextComponentValue<Double, NumberField<Double>> {

    private DoubleFieldValue(NumberField<Double> doubleField, boolean nullable, UpdateOn updateOn) {
      super(doubleField, nullable ? null : 0d, updateOn);
      if (!isNullable() && doubleField.getValue() == null) {
        doubleField.setValue(0d);
      }
    }

    @Override
    protected Double getComponentValue(NumberField<Double> component) {
      Number value = component.getValue();
      if (value == null) {
        return isNullable() ? null : 0d;
      }

      return value.doubleValue();
    }

    @Override
    protected void setComponentValue(NumberField<Double> component, Double value) {
      component.setValue(value);
    }
  }

  private static final class IntegerFieldValue extends AbstractTextComponentValue<Integer, NumberField<Integer>> {

    private IntegerFieldValue(NumberField<Integer> integerField, boolean nullable, UpdateOn updateOn) {
      super(integerField, nullable ? null : 0, updateOn);
      if (!isNullable() && integerField.getValue() == null) {
        integerField.setValue(0);
      }
    }

    @Override
    protected Integer getComponentValue(NumberField<Integer> component) {
      Number value = component.getValue();
      if (value == null) {
        return isNullable() ? null : 0;
      }

      return value.intValue();
    }

    @Override
    protected void setComponentValue(NumberField<Integer> component, Integer value) {
      component.setValue(value);
    }
  }

  private static final class LongFieldValue extends AbstractTextComponentValue<Long, NumberField<Long>> {

    private LongFieldValue(NumberField<Long> longField, boolean nullable, UpdateOn updateOn) {
      super(longField, nullable ? null : 0L, updateOn);
      if (!isNullable() && longField.getValue() == null) {
        longField.setValue(0L);
      }
    }

    @Override
    protected Long getComponentValue(NumberField<Long> component) {
      Number value = component.getValue();
      if (value == null) {
        return isNullable() ? null : 0L;
      }

      return value.longValue();
    }

    @Override
    protected void setComponentValue(NumberField<Long> component, Long value) {
      component.setValue(value);
    }
  }

  private static final class BigDecimalFieldValue extends AbstractTextComponentValue<BigDecimal, NumberField<BigDecimal>> {

    private BigDecimalFieldValue(NumberField<BigDecimal> doubleField, boolean nullable, UpdateOn updateOn) {
      super(doubleField, nullable ? null : BigDecimal.ZERO, updateOn);
    }

    @Override
    protected BigDecimal getComponentValue(NumberField<BigDecimal> component) {
      return component.getValue();
    }

    @Override
    protected void setComponentValue(NumberField<BigDecimal> component, BigDecimal value) {
      component.setValue(value);
    }
  }
}
