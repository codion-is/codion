/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
class FieldFormatter extends MaskFormatter {

  FieldFormatter(final String mask, final boolean valueContainsLiterals) throws ParseException {
    super(mask);
    setPlaceholderCharacter('_');
    setAllowsInvalid(false);
    setValueContainsLiteralCharacters(valueContainsLiterals);
  }

  @Override
  public final void install(final JFormattedTextField field) {
    final int previousLength = field.getDocument().getLength();
    final int currentCaretPosition = field.getCaretPosition();
    final int currentSelectionStart = field.getSelectionStart();
    final int currentSelectionEnd = field.getSelectionEnd();
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
