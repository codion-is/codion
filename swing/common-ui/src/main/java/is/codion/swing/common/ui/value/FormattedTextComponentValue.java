/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.Formats;

import javax.swing.JFormattedTextField;
import javax.swing.text.JTextComponent;
import java.text.Format;
import java.text.ParseException;

import static is.codion.common.Util.nullOrEmpty;

class FormattedTextComponentValue<V, C extends JTextComponent> extends AbstractTextComponentValue<V, C> {

  private final JFormattedTextField.AbstractFormatter formatter;
  private final Format format;

  FormattedTextComponentValue(final C textComponent, final Format format, final UpdateOn updateOn) {
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
  protected final V getComponentValue(final C component) {
    final String formattedText = getFormattedText(component);
    if (nullOrEmpty(formattedText)) {
      return null;
    }

    return parseValueFromText(formattedText);
  }

  @Override
  protected final void setComponentValue(final C component, final V value) {
    component.setText(value == null ? "" : formatTextFromValue(value));
  }

  /**
   * Returns a String representation of the given value object, using the format
   * Only called for non-null values.
   * @param value the value to return as String
   * @return a formatted String representation of the given value, an empty string if the value is null
   */
  protected String formatTextFromValue(final V value) {
    return format.format(value);
  }

  /**
   * Returns a property value based on the given text, if the text can not
   * be parsed into a valid value, null is returned.
   * Only called for non-null values.
   * @param text the text from which to parse a value
   * @return a value from the given text, or null if the parsing did not yield a valid value
   */
  protected V parseValueFromText(final String text) {
    try {
      return (V) format.parseObject(text);
    }
    catch (final ParseException e) {
      return null;
    }
  }

  private String getFormattedText(final C component) {
    try {
      final String text = component.getText();
      if (formatter == null) {
        return text;
      }

      return (String) formatter.stringToValue(text);
    }
    catch (final ParseException e) {
      return null;
    }
  }
}
