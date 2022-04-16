/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.event.EventDataListener;
import is.codion.common.formats.Formats;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.textfield.DocumentAdapter;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A text field for numbers.
 * Use the available factory methods for instances.
 * @param <T> the Number type
 */
public final class NumberField<T extends Number> extends JTextField {

  private final Value<T> value = Value.value();

  private NumberField(NumberDocument<T> document) {
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
   * @return Integer based {@link NumberField}
   */
  public static NumberField<Integer> integerField() {
    return integerField(Formats.getNonGroupingIntegerFormat());
  }

  /**
   * @param format the number format
   * @return Integer based {@link NumberField}
   */
  public static NumberField<Integer> integerField(NumberFormat format) {
    return new NumberField<>(new NumberDocument<>(new NumberParsingDocumentFilter<>(new NumberParser<>(format, Integer.class))));
  }

  /**
   * @return Double based {@link NumberField}
   */
  public static NumberField<Double> doubleField() {
    DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);

    return doubleField(format);
  }

  /**
   * @param format the number format
   * @return Double based {@link NumberField}
   */
  public static NumberField<Double> doubleField(DecimalFormat format) {
    return new NumberField<>(new DecimalDocument<>(format, false));
  }

  /**
   * @return Long based {@link NumberField}
   */
  public static NumberField<Long> longField() {
    return longField(Formats.getNonGroupingIntegerFormat());
  }

  /**
   * @param format the number format
   * @return Long based {@link NumberField}
   */
  public static NumberField<Long> longField(NumberFormat format) {
    return new NumberField<>(new NumberDocument<>(new NumberParsingDocumentFilter<>(new NumberParser<>(format, Long.class))));
  }

  /**
   * @return BigDecimal based {@link NumberField}
   */
  public static NumberField<BigDecimal> bigDecimalField() {
    DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);

    return bigDecimalField(format);
  }

  /**
   * @param format the number format
   * @return BigDecimal based {@link NumberField}
   */
  public static NumberField<BigDecimal> bigDecimalField(DecimalFormat format) {
    return new NumberField<>(new DecimalDocument<>(format, true));
  }

  /**
   * Can't override getDocument() with type cast since it's called before setting the document with a class cast exception.
   * @return the typed document.
   */
  NumberDocument<T> getTypedDocument() {
    return (NumberDocument<T>) super.getDocument();
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
