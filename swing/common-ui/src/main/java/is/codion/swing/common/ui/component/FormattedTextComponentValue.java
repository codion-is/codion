/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.formats.Formats;

import javax.swing.JFormattedTextField;
import javax.swing.text.JTextComponent;
import java.text.Format;
import java.text.ParseException;

import static is.codion.common.Util.nullOrEmpty;

final class FormattedTextComponentValue<T, C extends JTextComponent> extends AbstractTextComponentValue<T, C> {

  private final JFormattedTextField.AbstractFormatter formatter;
  private final Format format;

  FormattedTextComponentValue(C textComponent, Format format, UpdateOn updateOn) {
    super(textComponent, null, updateOn);
    if (textComponent instanceof JFormattedTextField) {
      this.formatter = ((JFormattedTextField) textComponent).getFormatter();
    }
    else {
      this.formatter = null;
    }
    this.format = format == null ? Formats.NULL_FORMAT : format;
  }

  @Override
  protected T getComponentValue(C component) {
    String formattedText = getFormattedText(component);
    if (nullOrEmpty(formattedText)) {
      return null;
    }

    return parseValueFromText(formattedText);
  }

  @Override
  protected void setComponentValue(C component, T value) {
    component.setText(value == null ? "" : format.format(value));
  }

  /**
   * Returns a value based on the given text, if the text can not be parsed into a valid value, null is returned.
   * Only called for non-null text.
   * @param text the text from which to parse a value
   * @return a value from the given text, or null if the parsing did not yield a valid value
   */
  private T parseValueFromText(String text) {
    try {
      return (T) format.parseObject(text);
    }
    catch (ParseException e) {
      return null;
    }
  }

  private String getFormattedText(C component) {
    try {
      String text = component.getText();
      if (formatter == null) {
        return text;
      }

      return (String) formatter.stringToValue(text);
    }
    catch (ParseException e) {
      return null;
    }
  }
}
