/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;

/**
 * A text field for numbers.
 * @param <T> the Number type
 */
public class NumberField<T extends Number> extends JTextField {

  /**
   * Specifies whether NumberFields disable grouping by default.<br>
   * Value type: Boolean<br>
   * Default value: false.
   */
  public static final PropertyValue<Boolean> DISABLE_GROUPING =
          Configuration.booleanValue("codion.swing.common.ui.disableNumberFieldGrouping", false);

  /**
   * Instantiates a new NumberField
   * @param document the document to use
   * @param columns the number of columns
   */
  public NumberField(final NumberDocument<T> document, final int columns) {
    super(document, null, columns);
    document.setCaret(getCaret());
    if (document.getFormat() instanceof DecimalFormat) {
      addKeyListener(new GroupingSkipAdapter());
    }
    if (DISABLE_GROUPING.get()) {
      document.getFormat().setGroupingUsed(false);
    }
  }

  /**
   * Set whether or not grouping will be used in this field.
   * @param groupingUsed true if grouping should be used false otherwise
   */
  public final void setGroupingUsed(final boolean groupingUsed) {
    getTypedDocument().getFormat().setGroupingUsed(groupingUsed);
  }

  /**
   * @param number the number to display in this field
   */
  public final void setNumber(final T number) {
    getTypedDocument().setNumber(number);
  }

  /**
   * @return the number being displayed in this field
   */
  public final T getNumber() {
    return getTypedDocument().getNumber();
  }

  /**
   * Sets the range of values this field should allow
   * @param min the minimum value
   * @param max the maximum value
   */
  public final void setRange(final double min, final double max) {
    getTypedDocument().getDocumentFilter().getNumberRangeValidator().setRange(min, max);
  }

  /**
   * @return the minimum value this field should accept
   */
  public final double getMinimumValue() {
    return getTypedDocument().getDocumentFilter().getNumberRangeValidator().getMinimumValue();
  }

  /**
   * @return the maximum value this field should accept
   */
  public final double getMaximumValue() {
    return getTypedDocument().getDocumentFilter().getNumberRangeValidator().getMaximumValue();
  }

  /**
   * Set the decimal and grouping separators for this field
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping separator
   * @throws IllegalArgumentException in case both separators are the same character
   */
  public final void setSeparators(final char decimalSeparator, final char groupingSeparator) {
    getTypedDocument().setSeparators(decimalSeparator, groupingSeparator);
  }

  /**
   * Can't override getDocument() with type cast since it's called before setting the document with a class cast exception.
   * @return the typed document.
   */
  protected final NumberDocument<T> getTypedDocument() {
    return (NumberDocument<T>) super.getDocument();
  }

  private final class GroupingSkipAdapter extends KeyAdapter {
    @Override
    public void keyReleased(final KeyEvent e) {
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

    private void skipGroupingSeparator(final boolean forward) {
      final NumberDocument<?> numberDocument = getTypedDocument();
      final char groupingSeparator = ((DecimalFormat) numberDocument.getFormat()).getDecimalFormatSymbols().getGroupingSeparator();
      try {
        final int caretPosition = getCaretPosition();
        if (forward && caretPosition < getDocument().getLength() - 1) {
          final char afterCaret = numberDocument.getText(caretPosition, 1).charAt(0);
          if (groupingSeparator == afterCaret) {
            setCaretPosition(caretPosition + 1);
          }
        }
        else if (!forward && caretPosition > 0) {
          final char beforeCaret = numberDocument.getText(caretPosition - 1, 1).charAt(0);
          if (groupingSeparator == beforeCaret) {
            setCaretPosition(caretPosition - 1);
          }
        }
      }
      catch (final BadLocationException ignored) {/*Not happening*/}
    }
  }
}
