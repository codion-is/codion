/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.event.EventDataListener;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.textfield.DocumentAdapter;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;

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
}
