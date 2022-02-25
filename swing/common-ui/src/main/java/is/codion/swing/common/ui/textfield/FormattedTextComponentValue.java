/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.formats.Formats;
import is.codion.swing.common.ui.component.AbstractTextComponentValue;
import is.codion.swing.common.ui.component.UpdateOn;

import javax.swing.JFormattedTextField;
import javax.swing.text.JTextComponent;
import java.text.Format;
import java.text.ParseException;

import static is.codion.common.Util.nullOrEmpty;

class FormattedTextComponentValue<V, C extends JTextComponent> extends AbstractTextComponentValue<V, C> {

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
  protected final V getComponentValue(C component) {
    String formattedText = getFormattedText(component);
    if (nullOrEmpty(formattedText)) {
      return null;
    }

    return parseValueFromText(formattedText);
  }

  @Override
  protected final void setComponentValue(C component, V value) {
    component.setText(value == null ? "" : formatTextFromValue(value));
  }

  /**
   * Returns a String representation of the given value object, using the format
   * Only called for non-null values.
   * @param value the value to return as String
   * @return a formatted String representation of the given value, an empty string if the value is null
   */
  protected String formatTextFromValue(V value) {
    return format.format(value);
  }

  /**
   * Returns a property value based on the given text, if the text can not
   * be parsed into a valid value, null is returned.
   * Only called for non-null values.
   * @param text the text from which to parse a value
   * @return a value from the given text, or null if the parsing did not yield a valid value
   */
  protected V parseValueFromText(String text) {
    try {
      return (V) format.parseObject(text);
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
