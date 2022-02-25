/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;

/**
 * Somewhat of a hack to keep the current field selection and caret position when
 * the field gains focus, in case the content length has not changed
 * http://stackoverflow.com/a/2202073/317760
 */
public final class FieldFormatter extends MaskFormatter {

  private FieldFormatter(String mask, boolean valueContainsLiterals) throws ParseException {
    super(mask);
    setPlaceholderCharacter('_');
    setAllowsInvalid(false);
    setValueContainsLiteralCharacters(valueContainsLiterals);
  }

  /**
   * Somewhat of a hacky way to keep the current field selection and caret position when
   * the field gains focus, in case the content length has not changed
   * http://stackoverflow.com/a/2202073/317760
   * @param mask the format mask
   * @param valueContainsLiterals true if the value should contain literals
   * @return a new MaskFormatter
   * @throws ParseException in case of an exception while parsing the mask
   */
  public static MaskFormatter create(String mask, boolean valueContainsLiterals) throws ParseException {
    return new FieldFormatter(mask, valueContainsLiterals);
  }

  @Override
  public void install(JFormattedTextField field) {
    int previousLength = field.getDocument().getLength();
    int currentCaretPosition = field.getCaretPosition();
    int currentSelectionStart = field.getSelectionStart();
    int currentSelectionEnd = field.getSelectionEnd();
    super.install(field);
    if (previousLength == field.getDocument().getLength()) {
      if (currentSelectionEnd - currentSelectionStart > 0) {
        field.setCaretPosition(currentSelectionStart);
        field.moveCaretPosition(currentSelectionEnd);
      }
      else {
        field.setCaretPosition(currentCaretPosition);
      }
    }
  }
}
