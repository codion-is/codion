/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.Value;
import org.jminor.common.value.Values;
import org.jminor.swing.common.ui.textfield.TextInputPanel;

import javax.swing.text.JTextComponent;
import java.text.Format;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * Utility class for string based {@link ComponentValue} instances.
 */
public final class StringValues {

  private StringValues() {}

  /**
   * @param textComponent the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextComponent> stringValue(final JTextComponent textComponent) {
    return new AbstractTextComponentValue<String, JTextComponent>(textComponent, true, true) {
      @Override
      protected String getComponentValue(final JTextComponent component) {
        final String text = component.getText();

        return nullOrEmpty(text) ? null : text;
      }
      @Override
      protected void setComponentValue(final JTextComponent component, final String value) {
        component.setText(value);
      }
    };
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextComponent> stringValue(final JTextComponent textComponent, final Format format) {
    return stringValue(textComponent, format, true);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<String, JTextComponent> stringValue(final JTextComponent textComponent, final Format format,
                                                                   final boolean updateOnKeystroke) {
    return new FormattedTextComponentValue<>(textComponent, format, updateOnKeystroke);
  }

  /**
   * Instantiates a new String based ComponentValue.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param initialValue the initial value
   * @param maxLength the maximum input length, -1 for no limit
   * @return a String based ComponentValue
   */
  public static ComponentValue<String, TextInputPanel> stringValue(final String inputDialogTitle, final String initialValue,
                                                                   final int maxLength) {
    return new TextInputPanelValue(inputDialogTitle, initialValue, maxLength);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param value the value to link with the component
   */
  public static void stringValueLink(final JTextComponent textComponent, final Value<String> value) {
    stringValueLink(textComponent, value, null, true);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param value the value to link with the component
   * @param format the format to use when displaying the linked value, null if no formatting should be performed
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void stringValueLink(final JTextComponent textComponent, final Value<String> value, final Format format,
                                     final boolean updateOnKeystroke) {
    Values.link(value, stringValue(textComponent, format, updateOnKeystroke));
  }
}
