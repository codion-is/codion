/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.Formats;
import org.jminor.swing.common.model.textfield.DocumentAdapter;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.Format;
import java.text.ParseException;

import static org.jminor.common.Util.nullOrEmpty;

class TextComponentValue<V, C extends JTextComponent> extends AbstractComponentValue<V, C> {

  private final JFormattedTextField.AbstractFormatter formatter;
  private final Format format;

  TextComponentValue(final C textComponent, final Format format, final boolean updateOnKeystroke) {
    super(textComponent);
    final Document document = textComponent.getDocument();
    if (textComponent instanceof JFormattedTextField) {
      this.formatter = ((JFormattedTextField) textComponent).getFormatter();
    }
    else {
      this.formatter = null;
    }
    this.format = format == null ? Formats.NULL_FORMAT : format;
    if (updateOnKeystroke) {
      document.addDocumentListener(new DocumentAdapter() {
        @Override
        public void contentsChanged(final DocumentEvent e) {
          notifyValueChange(get());
        }
      });
    }
    else {
      textComponent.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(final FocusEvent e) {
          if (!e.isTemporary()) {
            notifyValueChange(get());
          }
        }
      });
    }
  }

  @Override
  protected V getComponentValue(final C component) {
    return valueFromText(getText(component));
  }

  @Override
  protected final void setComponentValue(final C component, final V value) {
    component.setText(textFromValue(value));
  }

  /**
   * Returns a String representation of the given value object, using the format,
   * an empty string is returned in case of a null value
   * @param value the value to return as String
   * @return a formatted String representation of the given value, an empty string if the value is null
   */
  protected String textFromValue(final V value) {
    return value == null ? "" : format.format(value);
  }

  /**
   * Returns a property value based on the given text, if the text can not
   * be parsed into a valid value, null is returned
   * @param text the text from which to parse a value
   * @return a value from the given text, or null if the parsing did not yield a valid value
   */
  protected V valueFromText(final String text) {
    if (nullOrEmpty(text)) {
      return null;
    }

    try {
      return (V) format.parseObject(text);
    }
    catch (final ParseException e) {
      return null;
    }
  }

  private String getText(final C component) {
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
