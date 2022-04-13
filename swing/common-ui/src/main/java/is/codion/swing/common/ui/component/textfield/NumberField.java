/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.properties.PropertyValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.textfield.DocumentAdapter;

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
   * Specifies whether NumberFields use grouping by default.<br>
   * Value type: Boolean<br>
   * Default value: true.
   */
  public static final PropertyValue<Boolean> GROUPING_USED =
          Configuration.booleanValue("is.codion.swing.common.ui.component.textfield.NumberField.groupingUsed", true);

  private final Value<T> value = Value.value();

  /**
   * Instantiates a new NumberField
   * @param document the document to use
   * @param columns the number of columns
   */
  public NumberField(NumberDocument<T> document, int columns) {
    super(document, null, columns);
    document.setTextComponent(this);
    if (document.getFormat() instanceof DecimalFormat) {
      addKeyListener(new GroupingSkipAdapter());
    }
    document.getFormat().setGroupingUsed(GROUPING_USED.get());
    document.addDocumentListener((DocumentAdapter) e -> value.set(document.getNumber()));
  }

  /**
   * Set whether grouping will be used in this field.
   * @param groupingUsed true if grouping should be used false otherwise
   */
  public final void setGroupingUsed(boolean groupingUsed) {
    getTypedDocument().getFormat().setGroupingUsed(groupingUsed);
  }

  /**
   * @param number the number to display in this field
   */
  public final void setNumber(T number) {
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
  public final void setRange(double min, double max) {
    getTypedDocument().getDocumentFilter().setRange(min, max);
  }

  /**
   * @return the minimum value this field should accept
   */
  public final double getMinimumValue() {
    return getTypedDocument().getDocumentFilter().getMinimumValue();
  }

  /**
   * @return the maximum value this field should accept
   */
  public final double getMaximumValue() {
    return getTypedDocument().getDocumentFilter().getMaximumValue();
  }

  /**
   * Set the decimal and grouping separators for this field
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping separator
   * @throws IllegalArgumentException in case both separators are the same character
   */
  public final void setSeparators(char decimalSeparator, char groupingSeparator) {
    getTypedDocument().setSeparators(decimalSeparator, groupingSeparator);
  }

  /**
   * Sets the decimal separator
   * @param decimalSeparator the separator
   */
  public final void setDecimalSeparator(char decimalSeparator) {
    getTypedDocument().setDecimalSeparator(decimalSeparator);
  }

  /**
   * Sets the grouping separator
   * @param groupingSeparator the separator
   */
  public final void setGroupingSeparator(char groupingSeparator) {
    getTypedDocument().setGroupingSeparator(groupingSeparator);
  }

  /**
   * @param listener a listener notified when the value changes
   */
  public void addValueListener(EventDataListener<T> listener) {
    value.addDataListener(listener);
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
